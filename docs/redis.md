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

## RedisCacheService 封装

当前封装能力：

- `get(key)`：读取字符串值。
- `set(key, value, ttl)`：写入字符串值并设置过期时间。
- `setIfAbsent(key, value, ttl)`：仅当 key 不存在时写入，适合播放防刷和幂等控制。
- `increment(key, ttlWhenCreated)`：自增计数；如果 key 是新建的，则设置 TTL。
- `delete(key)`：删除缓存。

这些方法返回 `Optional` 或空结果，调用方可以根据 Redis 是否可用决定是否走 MySQL 回源或放行。

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

1. 接口限流：登录、注册、上传、评论、弹幕、点赞、投币、播放上报。
2. 播放防刷：用 `setIfAbsent` 做短期去重。
3. 首页 feed 和帖子详情短缓存。
4. 点赞、收藏、投币、播放等热点计数缓存。
5. Redis ZSet 榜单。

每接一个场景，都要补测试和缓存失效规则。
