# Ruru 视频转码与 HLS 播放说明

## 当前目标

Ruru 的视频链路正在从“直接播放用户上传的原始视频”升级为更接近 B 站的链路：

```text
用户上传原视频 -> R2 pending 区 -> ruru 审核通过
  -> 后端触发 FFmpeg 转码
  -> 生成 720P / 1080P HLS 文件
  -> 上传 HLS playlist 和 ts 分片到 R2
  -> 前端通过 hls.js 播放 master.m3u8
```

## 新增数据库能力

- `media_files.transcode_status`：视频转码状态，常见值为 `WAITING`、`TRANSCODING`、`READY`、`FAILED`。
- `media_files.transcode_error`：失败原因，方便管理员排查。
- `media_files.hls_master_object_key`：HLS master 文件逻辑位置。
- `media_transcode_variants`：每个视频有哪些清晰度，比如 `720P`、`1080P`。
- `media_transcode_segments`：每个清晰度下有哪些 ts 分片、分片时长和 R2 object key。

## 播放接口

- `GET /api/media/videos/{mediaFileId}/hls/master.m3u8`
- `GET /api/media/videos/{mediaFileId}/hls/{quality}/index.m3u8`
- `GET /api/media/videos/{mediaFileId}/hls/{quality}/segments/{segmentIndex}.ts`

分片接口不会直接公开 R2 密钥，会由后端生成短期签名 URL 后 302 跳转。

## 转码开关

默认不开启真实转码，避免你部署后没准备好 FFmpeg 或 R2 时误跑任务。

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

`MEDIA_QUEUE_FALLBACK_TO_LOCAL_EVENT=true` 的意思是：RabbitMQ 临时不可用时，先回退到本地事件，避免小流量阶段因为队列问题导致视频完全不能转码。后期真正生产化时，可以改成失败关闭并接入失败队列。

## 管理员能力

- ruru 可以审核视频。
- ruru 可以把失败的视频重新放回 `WAITING` 状态。
- 转码失败不会影响整个系统，只会把当前视频标记为 `FAILED` 并记录原因。

## 还没做完的后续增强

- 增加转码任务表，记录重试次数、开始时间、结束时间和 worker 日志。
- RabbitMQ 失败队列和重试次数治理。
- 接 CDN，让 HLS 分片播放更快。
- 增加上传次数/容量配额，防止恶意占用 R2。
