# Ruru 视频转码与 HLS 播放说明

## 完整转码链路

Ruru 的视频链路已实现完整的 B 站式转码播放流程：

```text
用户上传原视频 -> R2 pending 区 -> ruru 审核通过
  -> 后端触发 FFmpeg 转码
  -> 生成 480P / 720P / 1080P HLS 文件
  -> 上传 HLS playlist、ts 分片、master playlist 到 R2
  -> 前端通过 hls.js 播放 R2 上的 master.m3u8
  -> 转码未完成时，前端回退播放原始 MP4
```

## 转码状态流转

```text
PENDING (上传中)
  -> UPLOADED (上传完成，待审核)
  -> APPROVED -> WAITING (排队中)
  -> TRANSCODING (FFmpeg 处理中)
  -> READY (HLS 可用，前端切换 HLS 播放)
  或 FAILED (失败，前端继续播放 MP4 原片)

FAILED 状态可通过管理端重试 -> WAITING
```

## 清晰度变体

每个视频转码生成三种 HLS 清晰度，按分辨率递增排列：

| 清晰度 | 分辨率 | 码率 |
|--------|--------|------|
| 480P | 640×480 | 1000 kbps |
| 720P | 1280×720 | 2800 kbps |
| 1080P | 1920×1080 | 5000 kbps |

前端清晰度切换器按分辨率从低到高排列，默认自动选择（hls.js 自适应码率 ABR）。

## Master Playlist 上传 R2

转码完成后，`MediaTranscodeService` 生成 `#EXT-X-STREAM-INF` 格式的 master playlist，上传到 R2：

```
community/videos/{mediaFileId}/hls/master.m3u8
```

播放时 `GET /api/media/videos/{id}/hls/master.m3u8` 302 重定向到 R2 签名 URL，HLS 播放器直接从 R2 拉取，不经过业务服务器。Master playlist 包含三个清晰度的带宽和分辨率信息。

## 前端转码状态展示

视频详情页在播放器上方显示转码状态横幅：

| 状态 | 横幅颜色 | 文案 | 播放方式 |
|------|----------|------|----------|
| WAITING | 黄色 | 视频转码排队中，当前播放为原片画质 | 原始 MP4 |
| TRANSCODING | 蓝色 | 视频转码处理中，当前播放为原片画质 | 原始 MP4 |
| FAILED | 红色 | 视频转码失败（含原因），当前播放为原片画质 | 原始 MP4 |
| READY | 不显示 | — | HLS master playlist |

## MP4 原片回退

当 `transcodeStatus` 不是 `READY` 时，前端自动使用原始 MP4 URL（`/api/media/files/{id}` -> R2 签名 URL）播放。用户始终能看到视频内容，不会因为转码未完成而看到空白或错误。

转码完成后（`READY`），前端自动切换到 `hlsMasterUrl`，hls.js 接管播放并支持清晰度切换。

## 管理端转码管理

管理后台（`/circle/admin`）待审列表中：

- 每个视频显示转码状态标签：等待转码 / 转码中 / HLS 已就绪 / 转码失败
- 转码失败时展示 `transcodeError` 错误原因
- 转码失败时提供「重新转码」按钮，调用 `POST /api/admin/media/{id}/transcode/retry`
- 重试会清理旧的分片和变体记录，重置状态为 `WAITING`，重新触发转码

## 数据库能力

- `media_files.transcode_status`：视频转码状态，常见值为 `WAITING`、`TRANSCODING`、`READY`、`FAILED`。
- `media_files.transcode_error`：失败原因（最多 1000 字符），方便管理员排查。
- `media_files.hls_master_object_key`：HLS master 文件在 R2 上的 object key。
- `media_files.duration_seconds`：转码后估算的视频时长。
- `media_transcode_variants`：每个视频有哪些清晰度，含分辨率、码率、R2 object key。
- `media_transcode_segments`：每个清晰度下有哪些 ts 分片、分片时长和 R2 object key。

## 播放接口

- `GET /api/media/videos/{mediaFileId}/hls/master.m3u8` — 302 重定向到 R2 master playlist
- `GET /api/media/videos/{mediaFileId}/hls/{quality}/index.m3u8` — 从 DB 动态构建 variant playlist
- `GET /api/media/videos/{mediaFileId}/hls/{quality}/segments/{segmentIndex}.ts` — 302 重定向到 R2 分片签名 URL

分片接口不会直接公开 R2 密钥，后端生成短期签名 URL 后 302 跳转。

## 管理员接口

- `GET /api/admin/media/pending` — 待审视频列表（含转码状态）
- `POST /api/admin/media/{mediaFileId}/approve` — 审核通过，触发转码
- `POST /api/admin/media/{mediaFileId}/reject` — 驳回视频
- `POST /api/admin/media/{mediaFileId}/transcode/retry` — 重试失败转码

## 转码开关

默认不开启真实转码，避免部署后没准备好 FFmpeg 或 R2 时误跑任务。

`.env` 里需要开启：

```env
MEDIA_TRANSCODE_ENABLED=true
MEDIA_TRANSCODE_FFMPEG_PATH=ffmpeg
MEDIA_TRANSCODE_WORK_DIR=/tmp/ruru-transcode
MEDIA_TRANSCODE_TIMEOUT=30m
```

后端 Docker 镜像已经安装 `ffmpeg`。开启后，管理员审核通过视频或点击重试转码时，会触发后台异步任务。

## RabbitMQ 任务队列

默认情况下，系统仍然可以用本地 Spring 事件触发转码，方便开发和小流量部署。

如果要让转码任务进入 RabbitMQ，`.env` 中开启：

```env
MEDIA_QUEUE_ENABLED=true
RABBITMQ_DEFAULT_USER=ruru
RABBITMQ_DEFAULT_PASS=change-this-rabbitmq-password
MEDIA_QUEUE_EXCHANGE=ruru.media
MEDIA_QUEUE_TRANSCODE_QUEUE=ruru.media.transcode
MEDIA_QUEUE_TRANSCODE_ROUTING_KEY=media.transcode.requested
MEDIA_QUEUE_FALLBACK_TO_LOCAL_EVENT=true
```

开启后链路变成：

```text
审核通过或手动重试
  -> 后端发送 mediaFileId 到 RabbitMQ
  -> 转码消费者接收消息
  -> 调用 FFmpeg 生成 HLS
  -> 上传结果到 R2
  -> 写回转码状态
```

`MEDIA_QUEUE_FALLBACK_TO_LOCAL_EVENT=true` 的意思是：RabbitMQ 临时不可用时，先回退到本地事件，避免小流量阶段因为队列问题导致视频完全不能转码。

## 还没做完的后续增强

- 增加转码任务表，记录重试次数、开始时间、结束时间和 worker 日志。
- RabbitMQ 失败队列和重试次数治理。
- 接 CDN，让 HLS 分片播放更快。
- 增加上传次数/容量配额，防止恶意占用 R2。
