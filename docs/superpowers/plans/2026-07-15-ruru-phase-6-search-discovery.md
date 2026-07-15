# 阶段 6：搜索、榜单和推荐雏形

> **目标：** 让内容可以被找到、被排序、被推荐。完成后项目具备真正内容平台的发现能力，面试时可以讲搜索引擎、排行榜算法、推荐系统。

**创建时间：** 2026-07-14  
**状态：** `PLANNING`  
**预计工期：** 2-3 天

---

## 技术栈

- **后端：** Spring Boot 3、MyBatis-Plus、Redis（ZSet）、MySQL
- **前端：** React、TypeScript、TanStack Query、Ant Design
- **基础设施：** Redis（已有）、MySQL（已有）

---

## 当前已完成（基线）

| 功能 | 实现方式 | 文件 |
|------|----------|------|
| MySQL 搜索 | `LIKE` 匹配标题、正文、话题、作者 | [`CommunityPostService.searchPosts()`](backend/src/main/java/com/studyflow/community/post/CommunityPostService.java:127) |
| 热门榜单 | MySQL 加权 SQL：`viewCount×1 + reactionCount×3 + commentCount×2 + pigCount×5 + favoriteCount×4` | [`CommunityPostService.listHotRanking()`](backend/src/main/java/com/studyflow/community/post/CommunityPostService.java:155) |
| 前端搜索框 | `Input.Search` + 最新/热门切换 | [`CircleFeedPage.tsx`](frontend/src/pages/CircleFeedPage.tsx:137) |
| Redis 基础设施 | key 规范、缓存封装、限流、计数缓存 | [`RedisKeys`](backend/src/main/java/com/studyflow/infrastructure/redis/RedisKeys.java)、[`RedisCacheService`](backend/src/main/java/com/studyflow/infrastructure/redis/RedisCacheService.java) |

---

## 任务 6.1：Redis ZSet 热门榜单

**优先级：** 🔴 最高  
**状态：** `TODO`

### 背景

当前 `listHotRanking()` 每次请求都跑一条按加权字段 `ORDER BY` 的 SQL。随着数据量和访问量增长，每次扫全表排序的代价越来越高。Redis ZSet 天然适合排行榜 — 写入时更新 score，读取 O(log N)。

### 要做的事

#### 后端

1. **新增 Redis key 常量** `hot:ranking:all`（全站热门）和 `hot:ranking:{topicName}`（话题热门）
   - 文件：[`RedisKeys.java`](backend/src/main/java/com/studyflow/infrastructure/redis/RedisKeys.java)
   - ZSet member = `postId`（字符串），score = 加权分

2. **新增 `CommunityPostRankingService`**
   - 文件：`backend/src/main/java/com/studyflow/community/ranking/CommunityPostRankingService.java`
   - `updateRanking(postId)` — 从 MySQL 读当前 post 的计数，计算 score 写入 ZSet
   - `getHotRanking(page, pageSize)` — ZSet `ZREVRANGE` 分页读排行榜
   - `getTopicRanking(topicName, page, pageSize)` — 按话题过滤榜单
   - 每次点赞/投币/收藏/播放上报后调用 `updateRanking()`（写入时更新，避免读取时计算）

3. **修改 `listHotRanking()`**
   - 文件：[`CommunityPostService.java`](backend/src/main/java/com/studyflow/community/post/CommunityPostService.java:155)
   - 优先从 ZSet 读 postId 列表，再用 `communityPostMapper.selectBatchIds()` 查详情（避免全表排序）
   - Redis 不可用时降级回 MySQL 加权 SQL（兜底）

4. **权重公式**（与当前 MySQL 公式一致）：
   ```
   score = viewCount×1 + reactionCount×3 + commentCount×2 + pigCount×5 + favoriteCount×4
   ```

5. **冷启动**：启动时或 Redis 数据丢失时，从 MySQL 批量重建 ZSet（写一个 `rebuildRankings()` 方法）

#### 测试

- 文件：`backend/src/test/java/com/studyflow/community/CommunityRankingServiceTest.java`
- 验证 `updateRanking()` 正确计算 score
- 验证 ZSet 降序排列正确
- 验证 Redis 不可用时降级到 MySQL

#### 前端

- 无需改动 — 热门榜单接口路径不变，响应格式不变

### 验收标准

- [ ] 热门榜单优先从 Redis ZSet 读取
- [ ] 点赞/投币/收藏后 ZSet 分数即时更新
- [ ] Redis 挂掉时能回退 MySQL
- [ ] 冷启动重建 ZSet 功能可用
- [ ] 后端测试通过

### 涉及的 Redis key

```
ruru:hot:ranking:all          → ZSet  member=postId  score=加权分
ruru:hot:ranking:{topicName}  → ZSet  member=postId  score=加权分（三期扩展）
```

---

## 任务 6.2：关注流

**优先级：** 🔴 高  
**状态：** `TODO`

### 背景

当前首页 feed 展示全站内容，没有"只看关注的人"的选项。B 站/YouTube 的核心体验是关注流。

### 后端

1. **新增接口** `GET /community/feed/following`
   - 文件：[`CommunityPostController.java`](backend/src/main/java/com/studyflow/community/post/CommunityPostController.java)
   - 需要登录（`@AuthenticationPrincipal`）
   - 返回当前用户关注的用户发布的公开内容，按时间倒序

2. **新增 Service 方法** `listFollowingFeed(userId)`
   - 文件：[`CommunityPostService.java`](backend/src/main/java/com/studyflow/community/post/CommunityPostService.java)
   - 查询关注关系表（`user_follows`）拿到所有 `followingId`
   - `SELECT * FROM community_posts WHERE author_id IN (…) AND status = 'PUBLISHED' ORDER BY created_at DESC LIMIT 50`
   - 同一批 Redis 缓存（TTL 30s，因为关注流更个性化）

3. **缓存策略**：关注流用更短的 TTL（10-30 秒），因为关注关系变化和对方发帖都需要即时反映

#### 前端

1. **新增 API 调用**
   - 文件：[`community.ts`](frontend/src/api/community.ts)
   - `listFollowingFeed()` → `GET /community/feed/following`

2. **左侧导航增加"关注"按钮**
   - 文件：[`CircleFeedPage.tsx`](frontend/src/pages/CircleFeedPage.tsx)
   - 在 sidebar 加一个 `<FireOutlined /> 关注` 入口
   - 点击时调用 `listFollowingFeed()`，替换 feed 数据源
   - 游客点击 → 跳转登录页

### 验收标准

- [ ] 登录用户可以查看关注流
- [ ] 关注流只展示已关注用户的公开内容
- [ ] 关注流按时间倒序
- [ ] 游客看不到关注流入口
- [ ] 后端测试通过

---

## 任务 6.3：同话题相关推荐

**优先级：** 🟡 中  
**状态：** `TODO`

### 背景

视频详情页右侧的"更多视频"目前是 feed 中随机抽 5 个。应优先推荐同话题的内容。

### 后端

1. **新增 Service 方法** `listRelatedPosts(userId, postId)`
   - 文件：[`CommunityPostService.java`](backend/src/main/java/com/studyflow/community/post/CommunityPostService.java)
   - 逻辑：
     1. 根据 `postId` 找到当前帖子的 `topicId` 和 `topicName`
     2. 查询同话题 + 已发布的内容，排除自身，按创建时间倒序
     3. 如果同话题不足 5 个 → 用全站最新内容补齐
     4. 优先同话题 → 同 contentType → 全站
   - 返回 5-10 条

2. **新增接口** `GET /community/posts/{postId}/related`
   - 文件：[`CommunityPostController.java`](backend/src/main/java/com/studyflow/community/post/CommunityPostController.java)
   - 游客也可访问

#### 前端

1. **新增 API 调用**
   - 文件：[`community.ts`](frontend/src/api/community.ts)
   - `listRelatedPosts(postId)` → `GET /community/posts/{postId}/related`

2. **替换 `renderRelatedVideosCard()`**
   - 文件：[`PostDetailPage.tsx`](frontend/src/pages/PostDetailPage.tsx)
   - 从 `feedQuery` 过滤改为调用 `listRelatedPosts()`
   - 用 `useQuery(['community-related', postId], …)` 

### 验收标准

- [ ] 视频详情页右侧展示同话题相关视频
- [ ] 同话题不足时自动补齐
- [ ] 不推荐当前正在看的帖子自己
- [ ] 游客也能看到推荐

---

## 任务 6.4：用户行为记录

**优先级：** 🟡 中  
**状态：** `TODO`

### 背景

后续做更精准的推荐（协同过滤、热门加权）需要用户行为数据。先建立行为记录基础设施。

### 后端

1. **新增表 `user_behaviors`**
   - 文件：`V23__add_user_behaviors.sql`
   - 字段：`id, user_id, target_type (POST|TOPIC), target_id, action (VIEW|LIKE|FAVORITE|FOLLOW|SEARCH), created_at`
   - 索引：`(user_id, created_at)`、`(target_type, target_id, action)`

2. **新增实体和 Mapper**
   - `UserBehavior.java` + `UserBehaviorMapper.java`（MyBatis-Plus）

3. **新增 `UserBehaviorService`**
   - `record(userId, targetType, targetId, action)` — 异步记录行为（用 `@Async` 或同步写不阻塞主流程）
   - 行为触发点：
     - 打开视频详情页 → `VIEW` + postId
     - 点赞 → `LIKE` + postId
     - 收藏 → `FAVORITE` + postId
     - 关注用户 → `FOLLOW` + userId
     - 搜索 → `SEARCH` + keyword（可选存 `targetId=null, extra=keyword`）

4. **行为去重**：同一用户同一帖子同一行为 24 小时内只记录一次（用 Redis key `behavior:dedupe:{userId}:{targetId}:{action}` TTL 24h）

### 验收标准

- [ ] 打开视频、点赞、收藏、关注、搜索都有行为记录
- [ ] 同一行为 24h 不重复记录
- [ ] 行为记录不阻塞主流程
- [ ] `user_behaviors` 表可查询

---

## 任务 6.5：Elasticsearch 全文搜索（三期，先不做）

**优先级：** ⚪ 低  
**状态：** `LATER`

等 MySQL 搜索确实不够用（数据量 > 万级别、模糊搜索慢）时再接 Elasticsearch。当前 MySQL LIKE + 合理索引足够。

---

## 执行顺序

```
6.1 Redis ZSet 热门榜单     ← 第一天
6.2 关注流                  ← 第一天/第二天
6.3 同话题相关推荐           ← 第二天
6.4 用户行为记录            ← 第二天/第三天
```

## 每个任务做完后

1. 后端测试全绿：`cd backend && mvn clean test`
2. 前端 build 通过：`cd frontend && npm run build && npm run lint`
3. 提交 GitHub（小步提交，一个任务一个 commit）
4. CI/CD 自动部署
5. 线上验证功能可用
6. 更新本文档对应 checkbox

---

## 面试价值

完成后可以这样讲：

> 我实现了一个内容社区的发现系统，包括基于 Redis ZSet 的实时排行榜（点赞/投币/收藏实时更新分数）、关注流（只看关注用户的动态）、基于话题的相关推荐（同话题优先，冷启动时用全站最新补齐），以及用户行为记录（为后续协同过滤推荐做数据准备）。排行榜做了 Redis → MySQL 降级，Redis 不可用时不会影响页面可用性。
