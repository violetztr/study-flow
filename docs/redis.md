# Redis 设计说明

Ruru 当前把 Redis 作为生产级能力的基础设施，优先用于限流、防刷、短缓存和热点计数。MySQL 仍然是最终可信数据源，Redis 负责减轻数据库压力和保护接口。

## 基本原则

- 所有 Redis key 必须通过 `RedisKeys` 统一生成，不在业务代码里手写散落字符串。
- 所有 Redis 读写优先通过 `RedisCacheService` 封装，方便后续统一处理日志、降级和监控。
- Redis 是加速层，不是当前阶段的最终数据源。
- Redis 异常时采用失败开放策略：记录日志，核心浏览和业务尽量继续走 MySQL。
- 带状态的缓存必须设置 TTL，避免长期脏数据。

## Key 命名规范

统一前缀：

```text
ruru:
```

命名格式：

```text
ruru:{domain}:{action}:{identity}
```

常用 key：

| 场景 | 示例 | 说明 |
| --- | --- | --- |
| 接口限流 | `ruru:rate:login:127.0.0.1` | 登录限流，后续阶段接入 |
| 上传限流 | `ruru:rate:upload:user_7` | 用户上传预签名限流 |
| 播放防刷 | `ruru:view:dedupe:12:user_7` | 同一观看者对同一视频的去重 |
| 首页缓存 | `ruru:feed:video:0` | 视频频道第一页短缓存 |
| 帖子详情缓存 | `ruru:post:detail:9` | 帖子详情短缓存 |
| 帖子计数 | `ruru:counter:post:9` | 点赞、投币、收藏、播放等热点计数 |

`RedisKeys` 会把空格、冒号等不稳定字符规范化成下划线，避免 key 中出现难读、难排查的内容。

## 接口限流

当前限流通过 `RateLimitInterceptor` 统一拦截 `/api/**` 请求，不在每个 Controller 里手写判断。

限流规则：

| 接口 | action | 身份维度 | 窗口 | 阈值 | 目的 |
| --- | --- | --- | --- | --- | --- |
| `POST /api/auth/login` | `login` | IP | 1 分钟 | 8 次 | 防暴力破解 |
| `POST /api/auth/register` | `register` | IP | 5 分钟 | 5 次 | 防批量注册 |
| `POST /api/media/uploads/presign` | `upload` | 用户优先，未登录用 IP | 1 分钟 | 20 次 | 防恶意刷上传 URL |
| `POST /api/community/posts/{id}/comments` | `comment` | 用户优先，未登录用 IP | 1 分钟 | 12 次 | 防刷评论 |
| `POST /api/community/posts/{id}/danmaku` | `danmaku` | 用户优先，未登录用 IP | 1 分钟 | 30 次 | 防刷弹幕 |
| `POST /api/community/posts/{id}/reactions/like` | `like` | 用户优先，未登录用 IP | 1 分钟 | 60 次 | 防脚本刷点赞 |
| `POST /api/community/posts/{id}/reactions/pig` | `pig` | 用户优先，未登录用 IP | 1 分钟 | 20 次 | 防脚本刷投币 |
| `POST /api/community/posts/{id}/favorites` | `favorite` | 用户优先，未登录用 IP | 1 分钟 | 60 次 | 防脚本刷收藏 |
| `POST /api/community/posts/{id}/views` | `view` | 用户优先，未登录用 IP | 1 分钟 | 120 次 | 防刷播放上报 |

超过限制时返回：

```json
{
  "code": 429,
  "message": "请求太频繁，请稍后再试",
  "data": null
}
```

登录和注册使用 IP 维度，因为这两个接口通常还没有用户身份。社区互动和上传使用用户优先，是为了避免同一个 IP 下多个正常用户互相影响；如果没有登录态，就回退到 IP。

## RedisCacheService 封装

当前封装能力：

- `get(key)`：读取字符串值。
- `set(key, value, ttl)`：写入字符串值并设置过期时间。
- `setIfAbsent(key, value, ttl)`：仅当 key 不存在时写入，适合播放防刷和幂等控制。
- `increment(key, ttlWhenCreated)`：自增计数；如果 key 是新建的，则设置 TTL。
- `delete(key)`：删除缓存。

这些方法返回 `Optional` 或空结果，调用方可以根据 Redis 是否可用决定是否走 MySQL 回源或放行。

## 播放量防刷

播放量不是页面访问量，Ruru 当前只有在视频播放达到有效条件后才尝试计数：

- 播放时间达到 10 秒。
- 或者播放进度达到视频总时长的 20%。

有效播放上报会先写入 Redis 去重 key：

```text
ruru:view:dedupe:{postId}:{viewerKey}
```

示例：

```text
ruru:view:dedupe:12:user_7
```

这个 key 使用 `setIfAbsent` 写入，TTL 为 6 小时。含义是：同一用户或同一游客在短时间内重复上报同一个视频，只允许第一次继续尝试增加播放量。

当前播放链路：

1. 前端播放器上报 `playedSeconds` 和 `durationSeconds`。
2. 后端判断是否达到有效播放条件。
3. 有效播放先写 Redis 去重 key。
4. Redis 返回已存在时，本次不增加播放量。
5. Redis 不可用时，回退到 MySQL 的 `community_post_views` 去重记录。
6. MySQL 仍保存观看进度和观看历史，是最终可信来源。

注意：为了保证“观看历史”和“最大播放进度”不丢，Ruru 当前没有因为 Redis 命中就完全跳过 MySQL。Redis 这一阶段主要负责挡住重复计数和减少写计数压力；后续 feed/detail 缓存和互动计数缓存会继续降低读取压力。

## 失败开放策略

Redis 不应该成为当前阶段的单点故障。

例如：

- 首页 feed 缓存读取失败：直接查 MySQL。
- 视频详情缓存读取失败：直接查 MySQL。
- 限流 Redis 失败：先放行请求，并记录日志。
- 播放防刷 Redis 失败：回退到 MySQL 播放记录去重。

这样做的取舍是：

- 好处：Redis 挂了网站不至于立刻不可用。
- 坏处：Redis 异常期间抗刷和缓存能力下降。

当前 Ruru 用户量还小，这个取舍更现实。等后期流量变大，可以对登录、上传等高风险接口改成失败关闭。

## 后续接入顺序

1. 播放防刷：用 `setIfAbsent` 做短期去重。
2. 首页 feed 和帖子详情短缓存。
3. 点赞、收藏、投币、播放等热点计数缓存。
4. Redis ZSet 榜单。

每接一个场景，都要补测试和缓存失效规则。
