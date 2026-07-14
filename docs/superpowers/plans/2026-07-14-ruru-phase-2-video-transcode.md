# Ruru Phase 2 视频转码 / 清晰度切换 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全 HLS 转码链路的剩余缺口，让视频转码成为一个完整、可观测、可重试的生产级工作流。

**Architecture:** 后端 `MediaTranscodeService` 已具备核心转码能力（R2 下载 → FFmpeg → HLS → R2 上传），`MediaFile` 已有 `transcode_status`/`transcode_error` 字段，前端已有 `RuruVideoPlayer`（hls.js）和清晰度切换 UI 框架。本次聚焦补全缺口：480P 清晰度、master playlist 上传到 R2、前端转码状态展示、重试流程治理。

**Tech Stack:** Spring Boot, MyBatis-Plus, FFmpeg, HLS, Cloudflare R2, React, TypeScript, hls.js.

---

## 现有状态评估

| 能力 | 状态 | 说明 |
|---|---|---|
| FFmpeg 转码核心 | ✅ 已有 | `MediaTranscodeService.transcodeVideo()` |
| 720P / 1080P 变体 | ✅ 已有 | `VARIANTS` 列表 |
| **480P 变体** | ❌ 缺失 | 需新增 |
| HLS 分片上传 R2 | ✅ 已有 | `uploadVariant()` |
| **Master playlist 上传 R2** | ❌ 缺失 | 当前 `markReady()` 只写了 objectKey 但不生成/上传 master.m3u8 |
| Master playlist API | ✅ 已有 | `hlsMasterPlaylist()` 从 DB 动态构建 |
| **MP4 回退播放** | ❌ 缺失 | 前端未处理 TRANSCODING 状态时回退到原始 MP4 |
| 前端清晰度切换 UI | ✅ 框架已有 | `PostDetailPage.tsx` 有 `videoQualities` + `shouldShowQualitySwitch` |
| **前端转码状态展示** | ❌ 缺失 | 用户看不到 TRANSCODING / FAILED 状态 |
| 管理员重试 API | ✅ 已有 | `POST /api/admin/media/{id}/transcode/retry` |
| **重试原因展示** | ❌ 缺失 | FAILED 时 `transcodeError` 未在管理端展示 |
| RabbitMQ 异步 | ✅ 可选 | `MediaTranscodeTaskConsumer`，`MEDIA_QUEUE_ENABLED` 控制 |
| 本地事件回退 | ✅ 已有 | `@TransactionalEventListener` + `@Async` |

---

## File structure

- Modify: `backend/src/main/java/com/studyflow/media/MediaTranscodeService.java` — 新增 480P 变体，生成并上传 master playlist
- Modify: `backend/src/main/java/com/studyflow/media/MediaService.java` — master playlist 指向 R2 签名 URL
- Modify: `backend/src/main/java/com/studyflow/media/dto/MediaUploadCompleteResponse.java` — 增加 `transcodeVariants` 字段
- Modify: `frontend/src/api/media.ts` — 增加变体列表类型
- Modify: `frontend/src/pages/PostDetailPage.tsx` — 转码状态展示、MP4 回退、清晰度切换完善
- Modify: `frontend/src/pages/CommunityAdminPage.tsx` — 管理端展示转码失败原因
- Create: `backend/src/test/java/com/studyflow/media/MediaTranscodeServiceTest.java` — 转码服务单元测试
- Modify: `docs/media-transcode.md` — 更新转码文档

---

### Task 1: 新增 480P 清晰度变体 + 完善变体配置

**Files:**
- Modify: `backend/src/main/java/com/studyflow/media/MediaTranscodeService.java`

- [ ] **Step 1: 新增 480P 变体**

在 `MediaTranscodeService` 的 `VARIANTS` 列表中新增 `480P`（640×480, 1000kbps），放在 720P 之前，形成 `480P → 720P → 1080P` 三个清晰度：

```java
private static final List<VariantSpec> VARIANTS = List.of(
        new VariantSpec("480P", 640, 480, 1000),
        new VariantSpec("720P", 1280, 720, 2800),
        new VariantSpec("1080P", 1920, 1080, 5000)
);
```

- [ ] **Step 2: 按宽度排序 master playlist**

确保 `buildHlsMasterPlaylist`（在 `MediaService` 中）按分辨率从低到高排列变体，客户端默认选择第一个（最低清晰度），减少首次加载时间。

- [ ] **Step 3: 运行现有测试验证不破坏**

```bash
cd backend && mvn -B test
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/studyflow/media/MediaTranscodeService.java
git commit -m "feat: add 480P transcode variant"
```

---

### Task 2: 生成并上传 HLS master playlist 到 R2

**Files:**
- Modify: `backend/src/main/java/com/studyflow/media/MediaTranscodeService.java`
- Modify: `backend/src/main/java/com/studyflow/media/MediaService.java`

- [ ] **Step 1: 转码完成后生成 master.m3u8 并上传 R2**

在 `MediaTranscodeService.markReady()` 之前插入 `uploadMasterPlaylist()` 方法：

```java
private void uploadMasterPlaylist(S3Client s3Client, MediaFile mediaFile, 
        List<MediaTranscodeVariant> variants) {
    // 构建 master playlist 内容：
    // #EXTM3U
    // #EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=640x480
    // 480p/index.m3u8
    // ...
    String masterKey = "community/videos/%d/hls/master.m3u8".formatted(mediaFile.getId());
    uploadObject(s3Client, mediaFile.getBucketName(), masterKey, masterContent, CONTENT_TYPE_M3U8);
}
```

- [ ] **Step 2: 修改 markReady 调用链**

`transcodeVideo()` 中在循环结束后调用 `uploadMasterPlaylist()`，再调用 `markReady()`。

- [ ] **Step 3: 修改 hlsMasterPlaylist API**

当前 `MediaService.buildHlsMasterPlaylist()` 从 DB 动态拼接 manifest。改让它重定向到 R2 签名 URL，类似 `hlsSegment()` 的做法——这样 HLS 播放器直接从 R2 拉取，不经过业务服务器。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/studyflow/media/MediaTranscodeService.java backend/src/main/java/com/studyflow/media/MediaService.java
git commit -m "feat: upload HLS master playlist to R2"
```

---

### Task 3: 前端转码状态展示 + MP4 原片回退

**Files:**
- Modify: `frontend/src/api/media.ts`
- Modify: `frontend/src/pages/PostDetailPage.tsx`
- Modify: `frontend/src/components/community/RuruVideoPlayer.tsx`

- [ ] **Step 1: API 层增加转码变体信息**

`MediaUploadCompleteResponse` 增加字段：

```typescript
transcodeVariants?: Array<{
  qualityLabel: string
  width: number
  height: number
  playlistUrl: string
  status: string
}>
```

`MediaService.buildMediaResponse()` 在转码 READY 时附带变体列表和对应的 HLS URL。

- [ ] **Step 2: PostDetailPage 展示转码状态**

在视频播放区域上方增加状态横幅：
- `WAITING` → 黄色横幅："视频转码排队中，当前播放为原片画质"
- `TRANSCODING` → 蓝色横幅："视频转码处理中，当前播放为原片画质"
- `FAILED` → 红色横幅："视频转码失败，当前播放为原片画质"（管理员可见错误原因）
- `READY` → 不显示横幅，正常播放 HLS

- [ ] **Step 3: MP4 回退逻辑**

当前 `PostDetailPage` 获取视频 URL 后直接传给 `RuruVideoPlayer`。需要改为：
- `transcodeStatus === 'READY'` → 使用 `hlsMasterUrl`
- 其他状态 → 使用原始 MP4 URL（`/api/media/files/{id}` 302 重定向到 R2）

- [ ] **Step 4: 清晰度切换器改造**

当前 `videoQualities` 从前端的硬编码构建。改从后端返回的 `transcodeVariants` 构建，按分辨率排序，默认选中最低清晰度。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/media.ts frontend/src/pages/PostDetailPage.tsx
git commit -m "feat: show transcode status and MP4 fallback"
```

---

### Task 4: 管理端转码失败原因展示 + 重试流程完善

**Files:**
- Modify: `frontend/src/pages/CommunityAdminPage.tsx`
- Modify: `backend/src/main/java/com/studyflow/media/MediaService.java`

- [ ] **Step 1: 管理端待审列表增加转码状态列**

在视频审核列表中增加 `transcodeStatus` 和 `transcodeError` 两列，让管理员可以看到每个视频的转码状态。

- [ ] **Step 2: 失败状态的视频增加"重试转码"按钮**

视频 `transcodeStatus === 'FAILED'` 时，在操作列显示"重试转码"按钮和失败原因 tooltip。点击后调用已有的 `POST /api/admin/media/{id}/transcode/retry`。

- [ ] **Step 3: 后端重试接口完善**

当前 `MediaService.retryVideoTranscode()` 只检查媒体存在和视频类型。增加：
- 检查 `transcodeStatus` 是否为 `FAILED` 或 `WAITING`
- 重置转码状态为 `WAITING` 前记录重试次数（可选，为后续限流做准备）
- 返回明确的错误信息（如"该视频正在转码中，请等待完成"）

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/CommunityAdminPage.tsx backend/src/main/java/com/studyflow/media/MediaService.java
git commit -m "feat: show transcode failure reason and retry in admin"
```

---

### Task 5: 后端转码服务单元测试

**Files:**
- Create: `backend/src/test/java/com/studyflow/media/MediaTranscodeServiceTest.java`

- [ ] **Step 1: 编写转码服务测试**

测试以下场景：
- `transcodeVideo` 在媒体不存在时安全跳过
- `transcodeVideo` 在非视频类型时安全跳过
- `transcodeVideo` 在非 APPROVED 状态时安全跳过
- `markFailed` 正确截断超长错误信息（>1000 字符）
- `extension()` 正确识别 mp4/webm/mov/mkv 扩展名
- VARIANTS 列表包含 480P/720P/1080P 且按分辨率递增
- Master playlist 内容格式正确（`#EXTM3U` 开头，每个变体有 `#EXT-X-STREAM-INF`）

- [ ] **Step 2: 运行测试**

```bash
cd backend && mvn -B test
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/studyflow/media/MediaTranscodeServiceTest.java
git commit -m "test: cover transcode service validation and variants"
```

---

### Task 6: 更新转码文档

**Files:**
- Modify: `docs/media-transcode.md`

- [ ] **Step 1: 更新文档**

更新内容：
- 新增 480P 变体说明
- Master playlist R2 上传流程
- 前端转码状态展示说明
- MP4 回退机制说明
- 管理端重试流程
- 转码状态流转图：

```text
PENDING (上传中)
  → UPLOADED (上传完成，待审核)
  → APPROVED → WAITING (排队中)
  → TRANSCODING (FFmpeg 处理中)
  → READY (HLS 可用) 或 FAILED (失败)
  
FAILED 状态可通过管理端重试 → WAITING
```

- [ ] **Step 2: Commit**

```bash
git add docs/media-transcode.md
git commit -m "docs: update media transcode documentation"
```

---

### Task 7: 端到端验证 + 部署

**Files:**
- Verify: all Task 1–6 files

- [ ] **Step 1: 运行全量验证**

```bash
bash scripts/test-production-ops.sh && cd backend && mvn -B test && cd ../frontend && npm.cmd run build && npm.cmd run lint
```

- [ ] **Step 2: 本地 Docker 验证（如果环境支持）**

```bash
docker compose -f docker-compose.yml up -d --build
# 上传一个测试视频，观察转码日志
# 确认 480P/720P/1080P 三个清晰度均生成
# 确认前端可以切换清晰度
# 确认 MP4 回退在 TRANSCODING 状态时生效
```

- [ ] **Step 3: Push + 部署**

```bash
git push origin main
bash scripts/deploy-fast.sh <git-sha>
```

---

## 验收标准

- [ ] 上传视频后生成 480P / 720P / 1080P 三个 HLS 清晰度
- [ ] Master playlist 上传到 R2，播放器直接从 R2 拉取
- [ ] 转码处理中（WAITING/TRANSCODING），前端回退播放原始 MP4
- [ ] 转码完成后前端自动展示清晰度切换器
- [ ] 转码失败时前端展示红色错误状态，管理端可见错误原因
- [ ] 管理员可重试失败的转码任务
- [ ] 后端测试覆盖转码服务核心逻辑
- [ ] 文档描述完整转码链路

## 面试价值

FFmpeg 命令行参数设计、HLS 协议理解（master playlist + variant playlist + segments）、多清晰度变体建模、对象存储分片上传、异步任务状态机（WAITING → TRANSCODING → READY/FAILED）、前端 MP4/HLS 自适应回退、管理员重试治理。
