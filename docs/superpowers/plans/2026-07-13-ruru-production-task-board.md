# Ruru Production Task Board

> **For agentic workers:** This is the high-level task board for the next major Ruru phases. Do not implement all phases in one pass. For each phase, create a focused implementation plan before coding.

**Goal:** Upgrade Ruru from a small community project into a production-oriented Bilibili-style content platform.

**Architecture:** Keep the current Spring Boot + React modular monolith first. Strengthen deployment, video processing, messaging, discovery, realtime features, and observability before splitting services. Each phase must be independently deployable and explainable in interviews.

**Tech Stack:** Spring Boot, React, TypeScript, MySQL, Redis, RabbitMQ, FFmpeg, HLS, Cloudflare R2, Docker Compose, GitHub Actions, GHCR, Nginx, WebSocket.

---

## Phase 1: 部署生产化

**Purpose:** Stop building images on the server and make deployment reliable, fast, and recoverable.

**Core Tasks:**
- [ ] Confirm GitHub Actions builds backend/frontend Docker images and pushes them to GHCR.
- [ ] Confirm server uses `docker-compose.prod.yml` and `--no-build` to pull prebuilt images.
- [ ] Add or verify health checks for frontend, backend, MySQL, Redis, and RabbitMQ.
- [ ] Add database backup and restore commands.
- [ ] Add rollback instructions using the previous Git SHA or previous image tag.
- [ ] Document common production troubleshooting commands.

**Acceptance Criteria:**
- [ ] A push to `main` builds images in GitHub Actions.
- [ ] Server deploy uses `git pull`, `docker compose pull`, and `docker compose up -d --no-build`.
- [ ] Deployment no longer spends a long time downloading Maven/npm dependencies on the server.
- [ ] If deployment fails, there is a clear rollback path.
- [ ] There is a tested MySQL backup command.

**Interview Value:** CI/CD, Docker image registry, production deployment, rollback, backup, basic DevOps.

---

## Phase 2: 视频转码 / 清晰度切换

**Purpose:** Turn basic video upload into a real video platform workflow.

**Core Tasks:**
- [ ] Enable FFmpeg-based transcode flow in a controlled environment.
- [ ] Generate HLS output from uploaded MP4 videos.
- [ ] Produce multiple quality variants such as `480P`, `720P`, `1080P`.
- [ ] Store HLS manifest and segment metadata in MySQL.
- [ ] Upload generated HLS files to Cloudflare R2.
- [ ] Add frontend quality switcher.
- [ ] Keep original MP4 fallback while HLS is processing.
- [ ] Add transcode failure reason and retry flow.

**Acceptance Criteria:**
- [ ] Uploading a video creates a media record with transcode status.
- [ ] The video can play before HLS is ready using original MP4 fallback.
- [ ] After processing, the player can switch between available qualities.
- [ ] Failed transcode jobs show a clear error state.
- [ ] Admin can retry a failed transcode job.

**Interview Value:** FFmpeg, HLS, object storage, async processing, media metadata modeling.

---

## Phase 3: 私信系统

**Purpose:** Add user-to-user communication and make the community feel alive.

**Core Tasks:**
- [ ] Add private conversation data model.
- [ ] Add private message data model.
- [ ] Add conversation member read state or last-read marker.
- [ ] Add API for creating or opening a conversation.
- [ ] Add API for sending messages.
- [ ] Add API for paginated message history.
- [ ] Add unread count for each conversation.
- [ ] Add basic frontend pages: conversation list and chat detail.
- [ ] Add permission checks so users can only read their own conversations.

**Acceptance Criteria:**
- [ ] User A can send a message to User B.
- [ ] User B can see the conversation and reply.
- [ ] Message history supports pagination.
- [ ] Unread count changes after reading a conversation.
- [ ] A third user cannot access someone else's conversation.

**Interview Value:** conversation modeling, unread counts, pagination, access control, future WebSocket foundation.

---

## Phase 4: 搜索 / 热榜 / 关注流

**Purpose:** Improve content discovery after the site has more posts and videos.

**Core Tasks:**
- [ ] Add keyword search for title, topic, author, and collection.
- [ ] Add search result page with content type filters.
- [ ] Add Redis ZSet hot ranking for views, likes, pigs, comments, and favorites.
- [ ] Add hot feed API.
- [ ] Add following feed based on followed users.
- [ ] Add history page improvements for watched/read content.
- [ ] Document when to upgrade from MySQL search to Elasticsearch.

**Acceptance Criteria:**
- [ ] Users can search posts by keyword.
- [ ] Users can filter search results by video, article, and live.
- [ ] Hot page ranks content using a clear scoring formula.
- [ ] Following feed only shows content from followed users.
- [ ] Redis unavailable does not break the normal feed.

**Interview Value:** search, ranking, Redis ZSet, feed design, graceful degradation.

---

## Phase 5: 实时弹幕 / 通知

**Purpose:** Add realtime interaction without refreshing the page.

**Core Tasks:**
- [ ] Add WebSocket connection endpoint.
- [ ] Add authenticated WebSocket user binding.
- [ ] Push new danmaku messages to viewers in the same video room.
- [ ] Add notification model for comments, follows, likes, pigs, favorites, and private messages.
- [ ] Add unread notification count.
- [ ] Add frontend notification center.
- [ ] Add reconnect handling on the frontend.

**Acceptance Criteria:**
- [ ] Two browser windows watching the same video can see new danmaku in realtime.
- [ ] Users receive notification count updates without full page refresh.
- [ ] WebSocket reconnects after a temporary disconnect.
- [ ] Unauthorized users cannot subscribe to private channels.

**Interview Value:** WebSocket, realtime messaging, connection management, notification design.

---

## Phase 6: 直播

**Purpose:** Upgrade the reserved live channel into a demoable live module.

**Core Tasks:**
- [ ] Add live room table and live session table.
- [ ] Add live status: not started, live, ended.
- [ ] Integrate a simple RTMP/HLS live service such as SRS or Nginx-RTMP.
- [ ] Add live room page.
- [ ] Add live chat and live danmaku.
- [ ] Add Redis online viewer count.
- [ ] Add admin-only test live flow for `ruru`.

**Acceptance Criteria:**
- [ ] `ruru` can create a test live room.
- [ ] A viewer can open the live room page.
- [ ] Live chat or danmaku appears in realtime.
- [ ] Online count updates approximately correctly.

**Interview Value:** live streaming basics, RTMP/HLS, realtime chat, online count.

---

## Phase 7: 微服务拆分

**Purpose:** Split only after the monolith has clear module boundaries and real scaling needs.

**Core Tasks:**
- [ ] Document current module boundaries: user, content, media, interaction, notification, live.
- [ ] Extract shared DTO and API contracts.
- [ ] Identify the first service worth splitting, likely media service.
- [ ] Add service-to-service auth strategy.
- [ ] Add gateway or reverse proxy routing strategy.
- [ ] Decide service discovery approach: Nacos, Consul, or static Compose routing for the first step.
- [ ] Split one service as a proof of concept.

**Acceptance Criteria:**
- [ ] The monolith can still run as before before splitting.
- [ ] The first split service has its own Docker image.
- [ ] The main app can call the split service through a stable API.
- [ ] The deployment process remains understandable.
- [ ] The split solves a real problem, not just architectural decoration.

**Interview Value:** modular monolith, service boundaries, API contracts, gateway, service discovery, deployment complexity.

---

## Recommended Execution Order

1. 部署生产化
2. 视频转码 / 清晰度切换
3. 私信系统
4. 搜索 / 热榜 / 关注流
5. 实时弹幕 / 通知
6. 直播
7. 微服务拆分

## Operating Rule

For each phase:
- [ ] Write a focused implementation plan.
- [ ] Add tests before or alongside implementation.
- [ ] Implement in small commits.
- [ ] Verify locally.
- [ ] Push to GitHub.
- [ ] Deploy using the fast production flow.
- [ ] Write a short technical diary entry explaining the difficulty and solution.

