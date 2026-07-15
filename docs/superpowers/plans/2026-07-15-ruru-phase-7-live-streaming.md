# 阶段 7：直播基础

> **目标：** 把直播频道从占位升级成可演示的直播模块。完成后项目具备完整的内容平台三件套（图文 + 视频 + 直播），面试时可以讲实时音视频推拉流、WebSocket 长连接、在线人数统计。

**创建时间：** 2026-07-15  
**状态：** `PLANNING`  
**预计工期：** 2-3 天

---

## 技术栈

- **推流协议：** RTMP (OBS → SRS)
- **拉流协议：** HTTP-FLV（低延迟）/ HLS（兼容性好）
- **直播服务器：** SRS (Simple Realtime Server) 5.0+
- **实时消息：** WebSocket（Spring WebSocket / STOMP）
- **后端：** Spring Boot 3、MyBatis-Plus、Redis
- **前端：** React、TypeScript、flv.js / hls.js、WebSocket
- **基础设施：** Docker Compose（新增 SRS 容器）、Redis（在线人数）、MySQL（直播间表）

---

## 当前已完成（基线）

| 功能 | 实现方式 | 文件 |
|------|----------|------|
| 直播频道占位 | `channel === 'live'` 显示"直播准备中" | [`CircleFeedPage.tsx`](frontend/src/pages/CircleFeedPage.tsx:185) |
| 直播内容类型 | `contentType: 'LIVE'` 已预留 | [`CommunityPostResponse`](frontend/src/api/community.ts:46) |
| 弹幕系统 | 视频详情页已有弹幕发送/展示/颜色/开关 | [`PostDetailPage.tsx`](frontend/src/pages/PostDetailPage.tsx) |
| WebSocket 基础设施 | Spring 已集成 WebSocket 依赖 | `pom.xml` |
| Redis 基础设施 | key 规范、缓存封装、ZSet | [`RedisCacheService`](backend/src/main/java/com/studyflow/infrastructure/redis/RedisCacheService.java) |

---

## 任务 7.1：SRS 流媒体服务器接入

**优先级：** 🔴 最高  
**状态：** `TODO`

### 背景

当前直播频道是纯占位。需要接入流媒体服务器来支持真实的 RTMP 推流和 HTTP-FLV/HLS 拉流。SRS 是国产开源流媒体服务器，部署简单、文档齐全、社区活跃，比 Nginx-RTMP 更稳定。

### 要做的事

#### 基础设施

1. **Docker Compose 加入 SRS 容器**
   - 文件：[`docker-compose.yml`](docker-compose.yml)
   - 镜像：`ossrs/srs:5`
   - 端口映射：
     - `1935:1935` — RTMP 推流端口
     - `8085:8080` — HTTP-FLV / HLS 拉流端口
     - `1985:1985` — SRS API 端口
   - 挂载 SRS 配置文件
   - 健康检查

2. **SRS 配置文件**
   - 文件：`docker/srs/srs.conf`
   - RTMP 监听 1935
   - HTTP-FLV 分发
   - HLS 分发（作为兜底，延迟更高但兼容性更好）
   - HTTP API 开启（用于查询流状态）

#### 后端

3. **SRS 回调接口**
   - 文件：`backend/src/main/java/com/studyflow/live/SrsCallbackController.java`
   - `POST /api/live/srs/on_publish` — 推流开始时回调，更新直播间状态为"直播中"
   - `POST /api/live/srs/on_unpublish` — 推流结束时回调，更新直播间状态为"已结束"
   - 安全校验：回调请求必须来自 SRS 容器内网 IP

4. **推流密钥管理**
   - 文件：`backend/src/main/java/com/studyflow/live/LiveStreamService.java`
   - 每个用户生成唯一推流密钥（`stream_key`）
   - 推流地址：`rtmp://服务器IP:1935/live/{streamKey}`
   - OBS 配置时使用这个地址

#### 前端

5. **直播播放器**
   - 文件：`frontend/src/components/community/LivePlayer.tsx`
   - 使用 `flv.js` 播放 HTTP-FLV（首选项，延迟 2-3 秒）
   - 使用 `hls.js` 兜底（iOS Safari 兼容，延迟 10-15 秒）
   - 播放器控制：播放/暂停、音量、全屏

### 验收标准

- [ ] Docker Compose 启动后 SRS 容器健康运行
- [ ] OBS 可以 RTMP 推流到 SRS
- [ ] 前端可以用 flv.js 播放直播流
- [ ] 推流开始/结束时后端自动感知并更新状态
- [ ] iOS Safari 可以走 HLS 兜底播放

---

## 任务 7.2：直播间数据模型和基础接口

**优先级：** 🔴 最高  
**状态：** `TODO`

### 背景

当前没有直播间表，直播流无法和社区内容体系打通。需要新建直播间表，让直播间能像普通帖子一样出现在频道、有标题封面、有关联话题。

### 后端

1. **新增 Flyway 迁移 V24**
   - 文件：`V24__add_live_rooms.sql`
   - 表 `live_rooms`：
     ```sql
     id              BIGINT PRIMARY KEY AUTO_INCREMENT
     user_id         BIGINT NOT NULL              -- 主播
     circle_id       BIGINT NOT NULL              -- 所属圈子
     title           VARCHAR(200) NOT NULL        -- 直播标题
     cover_url       VARCHAR(500)                 -- 封面图
     topic_id        BIGINT                       -- 话题
     topic_name      VARCHAR(100)                 -- 话题名（冗余）
     stream_key      VARCHAR(64) NOT NULL UNIQUE  -- 推流密钥
     status          VARCHAR(20) NOT NULL DEFAULT 'WAITING'  -- WAITING / LIVE / ENDED
     started_at      DATETIME                     -- 开播时间
     ended_at        DATETIME                     -- 下播时间
     peak_viewers    INT DEFAULT 0                -- 峰值在线人数
     total_views     INT DEFAULT 0                -- 累计观看人次
     created_at      DATETIME NOT NULL
     updated_at      DATETIME NOT NULL
     ```
   - 索引：`(circle_id, status)`、`(user_id, created_at)`

2. **新增实体和 Mapper**
   - `LiveRoom.java` + `LiveRoomMapper.java`（MyBatis-Plus）

3. **新增 `LiveRoomService`**
   - 文件：`backend/src/main/java/com/studyflow/live/LiveRoomService.java`
   - `createLiveRoom(userId, request)` — 创建直播间（标题、封面、话题）
   - `startLive(streamKey)` — SRS 回调触发，状态 → LIVE
   - `endLive(streamKey)` — SRS 回调触发，状态 → ENDED
   - `listLiveRooms(circleId)` — 列出正在直播的房间
   - `getLiveRoom(roomId)` — 直播间详情

4. **新增 Controller**
   - 文件：`backend/src/main/java/com/studyflow/live/LiveRoomController.java`
   - `POST /api/live/rooms` — 创建直播间
   - `GET /api/live/rooms` — 正在直播的房间列表
   - `GET /api/live/rooms/{roomId}` — 直播间详情（含推流地址，仅主播可见）
   - `PUT /api/live/rooms/{roomId}` — 更新直播间信息

5. **直播流地址生成**
   - 拉流地址（前端播放）：
     - HTTP-FLV：`http://{host}:8085/live/{streamKey}.flv`
     - HLS：`http://{host}:8085/live/{streamKey}/index.m3u8`
   - 需要对用户隐藏真实 SRS 地址，由后端返回

#### 前端

6. **新增 API**
   - 文件：[`community.ts`](frontend/src/api/community.ts)
   - `listLiveRooms()` → `GET /live/rooms`
   - `getLiveRoom(roomId)` → `GET /live/rooms/{roomId}`
   - `createLiveRoom(request)` → `POST /live/rooms`

7. **修改直播频道**
   - 文件：[`CircleFeedPage.tsx`](frontend/src/pages/CircleFeedPage.tsx:185)
   - 从"直播准备中"改为真实直播卡片列表
   - 直播卡片显示：标题、封面、主播、在线人数、开播时长
   - 点击进入直播间详情页

8. **新增直播间详情页**
   - 文件：`frontend/src/pages/LiveRoomPage.tsx`
   - 顶部：直播播放器
   - 右侧：主播信息、在线人数、聊天/弹幕区
   - 路由：`/circle/live/{roomId}`

### 验收标准

- [ ] 用户可以创建直播间，填写标题和封面
- [ ] 直播列表只显示正在直播的房间
- [ ] 直播结束自动更新状态
- [ ] 前端直播频道展示真实直播卡片
- [ ] 直播间详情页有播放器和基础信息

---

## 任务 7.3：直播间实时聊天和弹幕

**优先级：** 🟡 高  
**状态：** `TODO`

### 背景

直播间必须支持实时互动。弹幕系统在视频详情页已有基础（REST API + 轮询），直播间需要升级为 WebSocket 推送，实现真正的实时弹幕。

### 后端

1. **WebSocket 配置**
   - 文件：`backend/src/main/java/com/studyflow/config/WebSocketConfig.java`
   - 配置 STOMP over WebSocket
   - 端点：`/ws`
   - 消息代理：`/topic`（广播）、`/queue`（点对点）

2. **直播弹幕和聊天消息**
   - 同一张 `community_danmaku` 表复用（需要加 `room_id` 字段或新表）
   - **建议：** 新建 `live_messages` 表，不混用视频弹幕
     - `id, room_id, user_id, content, color, type (CHAT|DANMAKU|SYSTEM), created_at`
   - 消息通过 WebSocket 实时广播

3. **消息处理**
   - 文件：`backend/src/main/java/com/studyflow/live/LiveMessageController.java`
   - `@MessageMapping("/live/{roomId}/chat")` — 接收聊天消息
   - `@MessageMapping("/live/{roomId}/danmaku")` — 接收弹幕消息
   - 服务端广播到 `/topic/live/{roomId}`
   - 持久化到 `live_messages` 表

4. **消息频率限制**
   - 复用现有的 [`RateLimitInterceptor`](backend/src/main/java/com/studyflow/infrastructure/ratelimit/RateLimitInterceptor.java)
   - 弹幕：每 2 秒 1 条
   - 聊天：每 1 秒 1 条

#### 前端

5. **WebSocket 连接**
   - 使用 `@stomp/stompjs` + SockJS
   - 进入直播间时建立连接，离开时断开
   - 订阅 `/topic/live/{roomId}` 接收实时消息

6. **聊天组件**
   - 文件：`frontend/src/components/community/LiveChat.tsx`
   - 聊天消息列表（滚动到底部）
   - 输入框 + 发送按钮
   - 区分普通聊天和系统消息（"XXX 进入直播间"）

7. **弹幕组件**
   - 文件：`frontend/src/components/community/LiveDanmaku.tsx`
   - 复用视频弹幕的 CSS 动画（从右到左飘过）
   - 弹幕颜色选择
   - 弹幕开关

### 验收标准

- [ ] 直播间可以发聊天和弹幕
- [ ] 消息通过 WebSocket 实时推送给所有观众
- [ ] 弹幕以飘屏动画展示
- [ ] 消息频率有限制，不能刷屏
- [ ] 聊天和弹幕持久化到数据库

---

## 任务 7.4：在线人数统计

**优先级：** 🟡 高  
**状态：** `TODO`

### 背景

直播的核心指标之一是在线人数。用 Redis 实现简单但可靠的在线统计。

### 后端

1. **Redis 在线人数**
   - Key：`ruru:live:viewers:{roomId}` → Set / ZSet
   - 用户进入直播间：`SADD ruru:live:viewers:{roomId} {userId_or_sessionId}`
   - 用户离开直播间：`SREM ruru:live:viewers:{roomId} {userId_or_sessionId}`
   - 获取在线人数：`SCARD ruru:live:viewers:{roomId}`
   - 游客用 sessionId 代替 userId

2. **心跳机制**
   - 前端每 15 秒发一次心跳（通过 WebSocket 或 HTTP）
   - 后端用 ZSet 存心跳时间戳：
     - `ZADD ruru:live:heartbeat:{roomId} {timestamp} {userId}`
   - 定时任务每 30 秒清理超过 30 秒无心跳的用户：
     - `ZREMRANGEBYSCORE ruru:live:heartbeat:{roomId} 0 {now-30s}`
     - 同步清理 viewers Set

3. **峰值在线人数**
   - 每次心跳时检查当前在线数是否超过 `live_rooms.peak_viewers`
   - 超过则更新 MySQL

4. **直播结束清理**
   - SRS `on_unpublish` 回调触发
   - 删除 Redis 中的 viewers 和 heartbeat key
   - 记录最终累计观看人次到 MySQL

#### 前端

5. **心跳发送**
   - 文件：[`LiveRoomPage.tsx`](frontend/src/pages/LiveRoomPage.tsx)
   - 每 15 秒通过 WebSocket 发送心跳帧
   - 页面关闭/离开时通过 `beforeunload` + WebSocket 发送离开事件

6. **在线人数展示**
   - 直播间顶部/右侧展示实时在线人数
   - 人数变化时做数字动画

### 验收标准

- [ ] 进入直播间后在线人数 +1
- [ ] 离开/关闭页面后在线人数 -1
- [ ] 30 秒无心跳自动剔除
- [ ] 峰值在线人数正确记录
- [ ] 直播结束后 Redis key 被清理

---

## 任务 7.5：直播回放和录播

**优先级：** ⚪ 低（可延后）  
**状态：** `LATER`

### 背景

直播结束后，用户可能想看回放。一期可以不做，先手动把 SRS 录制的文件转成普通视频投稿。

### 大致方案（先不实现）

- SRS 配置录制 → 直播结束时生成 FLV/MP4 文件
- 上传到 R2
- 自动创建一条 `contentType=VIDEO` 的投稿
- 关联原直播间 ID

---

## 执行顺序

```
7.1 SRS 流媒体服务器接入      ← 第一天上午
7.2 直播间数据模型和基础接口   ← 第一天下午
7.3 实时聊天和弹幕            ← 第二天
7.4 在线人数统计              ← 第二天/第三天
7.5 直播回放                  ← 延后
```

---

## Nginx 配置调整

直播部署后需要在服务器 Nginx 中增加 SRS 拉流代理，避免跨端口访问：

```nginx
# 在现有的 violet server block 中添加
location /live/ {
    proxy_pass http://127.0.0.1:8085/live/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    
    # FLV 需要长连接支持
    proxy_http_version 1.1;
    proxy_set_header Connection "";
}
```

这样前端统一走 `https://www.violet-surf.com/live/{streamKey}.flv`，不用暴露 SRS 端口。

---

## 每个任务做完后

1. 后端测试全绿：`cd backend && mvn clean test`
2. 前端 build 通过：`cd frontend && npm run build && npm run lint`
3. 提交 GitHub（小步提交，一个任务一个 commit）
4. CI/CD 自动部署
5. 用 OBS 推流测试验证
6. 更新本文档对应 checkbox

---

## OBS 配置步骤

1. 在 ruru 网站创建直播间 → 获取推流地址和密钥
2. OBS → 设置 → 推流：
   - 服务：`自定义`
   - 服务器：`rtmp://45.56.91.109:1935/live`
   - 串流密钥：`{你的 streamKey}`
3. 开始推流 → 直播间状态自动变为"直播中"
4. 前端刷新即可看到直播

---

## 面试价值

完成后可以这样讲：

> 我实现了一个完整的直播模块，包括基于 SRS 的 RTMP 推流和 HTTP-FLV 低延迟拉流（2-3 秒延迟），基于 WebSocket + STOMP 的实时聊天和弹幕系统，以及基于 Redis ZSet + 心跳机制的在线人数统计（30 秒无心跳自动剔除）。直播状态通过 SRS 回调自动感知，推流开始时 on_publish 触发数据库状态变更，结束时 on_unpublish 清理 Redis 并记录峰值数据。整个系统通过 Docker Compose 一键部署，前端统一通过 Nginx 反向代理访问，不暴露内部端口。
