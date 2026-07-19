# Ruru 模块架构文档

> 阶段 9：模块化单体升级 — 架构全景图  
> 更新日期: 2026-07-19

---

## 1. 模块全景

```
com.studyflow
├── auth              # 认证模块（登录、注册）
├── user              # 用户领域（User 实体 + UserMapper + UserResponse DTO）
├── community         # 内容社区模块（最大模块，含 14 个子包）
├── live              # 直播模块（直播间、聊天、弹幕、SRS 回调）
├── media             # 媒体模块（上传、R2 存储、转码状态）
├── wallet            # 钱包模块（猪币、每日奖励）
├── security          # 安全基础设施（JWT、Spring Security 配置）
├── config            # 全局配置（WebMvc、WebSocket、OpenAPI）
├── infrastructure    # 基础设施（Redis、限流、traceId 日志）
└── common            # 公共模型（ApiResponse、BusinessException、全局异常处理）
```

### 1.1 模块职责速查

| 模块 | 职责 | 核心类 | 未来可拆为 |
|------|------|--------|-----------|
| [`auth`](backend/src/main/java/com/studyflow/auth) | 登录、注册、JWT 签发 | `AuthService`, `AuthController` | 合并到 user-service |
| [`user`](backend/src/main/java/com/studyflow/user) | 用户实体、基础查询 | `User`, `UserMapper`, `UserResponse` | `user-service` |
| [`community`](backend/src/main/java/com/studyflow/community) | 帖子、评论、弹幕、点赞、收藏、关注、审核、榜单 | 14 个子 Service | `community-service` |
| [`live`](backend/src/main/java/com/studyflow/live) | 直播间 CRUD、聊天、弹幕、在线人数、SRS 回调 | `LiveRoomService`, `LiveMessageService` | `live-service` |
| [`media`](backend/src/main/java/com/studyflow/media) | 上传预签名 URL、R2 存储、转码状态 | `MediaService`, `MediaController` | `media-service` |
| [`wallet`](backend/src/main/java/com/studyflow/wallet) | 猪币余额、每日奖励、投币收币 | `PigWalletService` | `wallet-service` |
| [`security`](backend/src/main/java/com/studyflow/security) | JWT 解析、认证过滤器、SecurityConfig | `JwtService`, `JwtAuthenticationFilter` | 保留在 infrastructure |
| [`config`](backend/src/main/java/com/studyflow/config) | WebMvc、WebSocket、OpenAPI 配置 | `WebMvcConfig`, `WebSocketConfig` | 保留在 infrastructure |
| [`infrastructure`](backend/src/main/java/com/studyflow/infrastructure) | Redis 缓存、限流、traceId | `RedisCacheService`, `RateLimitInterceptor` | 保留在 infrastructure |
| [`common`](backend/src/main/java/com/studyflow/common) | ApiResponse、BusinessException、全局异常处理 | `ApiResponse`, `GlobalExceptionHandler` | 保留在 infrastructure |

---

## 2. community 子模块结构

社区模块是当前最大的模块，内部已按领域拆分为 14 个子包：

```
community/
├── background/       # 个人主页背景预设
├── circle/           # 圈子（Circle）实体
├── collection/       # 收藏夹
├── comment/          # 评论
├── counter/          # 互动计数（点赞数、收藏数、评论数）
├── danmaku/          # 视频弹幕
├── discovery/        # 发现页（feed、搜索）
├── favorite/         # 收藏功能
├── fandom/           # 关注/粉丝
├── foundation/       # 话题（Topic）基础数据
├── member/           # 圈子成员、UserProfile
├── moderation/       # 审核治理
├── post/             # 帖子（CRUD、展示）
├── ranking/          # 热门榜单
└── reaction/         # 点赞、投币（PigLike）
```

---

## 3. 跨模块依赖矩阵

### 3.1 直接查表依赖（Mapper 调用）

| 调用方模块 | 被调用的 Mapper | 所属模块 | 风险等级 | 说明 |
|-----------|----------------|---------|---------|------|
| `auth` | `UserMapper` | user | 🟢 低 | 认证本身就需要查用户表 |
| `security` | `UserMapper` | user | 🟢 低 | JWT 过滤器查用户加载 Principal |
| `community.*` (6 处) | `UserMapper` | user | 🟡 中 | 获取用户名/信息展示，未来应通过 `UserService` 而非直接查 Mapper |
| `live` | `UserMapper` | user | 🟡 中 | `getLiveRoom()` 查用户名和头像 |
| `live` | `UserProfileMapper` | community.member | 🟡 中 | 查用户头像，应通过 community 模块的 Service |
| `media` | `UserMapper` | user | 🟢 低 | 上传者身份校验 |

### 3.2 Service 调用依赖

| 调用方模块 | 被调用的 Service | 所属模块 | 风险等级 | 说明 |
|-----------|-----------------|---------|---------|------|
| `auth` | `CommunityMemberService` | community.member | 🟡 中 | 注册时自动加入默认圈子 |
| `auth` | `PigWalletService` | wallet | 🟢 低 | 注册时初始化钱包 |
| `live` | `CommunityMemberService` | community.member | 🟡 中 | 创建直播间时校验圈子成员 |
| `community.reaction` | `PigWalletService` | wallet | 🟢 低 | 投币时扣币/加币 |

### 3.3 依赖方向总览

```
        ┌──────────────────────────────────────┐
        │              common                   │
        │  (ApiResponse, BusinessException)     │
        └──────────────────────────────────────┘
                        ↑ (所有模块依赖)
        ┌───────────────┼───────────────┐
        │               │               │
   ┌────┴────┐    ┌────┴────┐    ┌─────┴─────┐
   │  auth   │    │ config  │    │infrastructure│
   └────┬────┘    └─────────┘    └───────────┘
        │ (注册时调用)
   ┌────┴────────────────────────────┐
   │        community                │
   │  (最大模块，14 子包)              │
   └─────────────────────────────────┘
        │              │              │
   ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
   │  live   │   │  media  │   │ wallet  │
   └─────────┘   └─────────┘   └─────────┘
        ↑              ↑              ↑
   ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
   │  user   │   │  user   │   │  user   │
   └─────────┘   └─────────┘   └─────────┘
```

**核心规则**：
- `user` 模块是所有模块的公共依赖（最底层业务模块）
- `community` 是业务中心，被 `auth`、`live` 依赖
- `wallet` 被 `auth` 和 `community.reaction` 调用
- `common`、`config`、`infrastructure`、`security` 是横向基础设施层

---

## 4. 模块边界规则

### 4.1 当前状态：✅ 良好

- **没有跨模块直接查非 user 模块的 Mapper**：除了 `UserMapper` 被多模块引用外，没有发现 `community` 直接查 `live` 的表、`media` 直接查 `wallet` 的表等乱象。
- **Service 调用已规范**：跨模块调用主要通过 Service 接口（如 `PigWalletService`、`CommunityMemberService`），而非直接操作 Mapper。
- **DTO 隔离**：每个模块的 DTO 放在自己的 `dto/` 子包中（如 `user.dto.UserResponse`、`wallet.dto.UserWalletResponse`）。

### 4.2 改进建议（未来迭代）

| 问题 | 当前状态 | 建议 |
|------|---------|------|
| `UserMapper` 被 6+ 个模块直接引用 | 🟡 中风险 | 未来封装 `UserService` 提供 `getUserById()` 等方法，禁止外部直接查 `UserMapper` |
| `UserProfileMapper` 被 `live` 模块直接引用 | 🟡 中风险 | 通过 `CommunityMemberService` 或新建 `UserProfileService` 暴露接口 |
| community 子包之间互相引用 | 🟢 低风险 | 同模块内允许，但建议核心子包（post/comment/reaction）保持清晰的单向依赖 |

---

## 5. 未来微服务拆分建议

当单体复杂度达到临界点时，按以下顺序拆分：

### 5.1 推荐拆分顺序

1. **`media-service`**（最先拆）
   - 理由：上传、转码、R2 存储自成一体，与业务逻辑耦合最低
   - 对外接口：上传预签名、转码状态查询、HLS 分片获取

2. **`live-service`**
   - 理由：WebSocket 连接、SRS 回调、在线人数统计，技术栈独立
   - 对外接口：直播间 CRUD、聊天消息、弹幕推送

3. **`wallet-service`**
   - 理由：猪币系统独立，被少量模块调用
   - 对外接口：余额查询、投币、收币、每日奖励

4. **`community-service`**
   - 理由：核心业务最大，最后拆
   - 对外接口：帖子、评论、弹幕、点赞、收藏、关注、审核、榜单

5. **`user-service`**
   - 理由：被所有模块依赖，待其他模块拆完后最后拆
   - 对外接口：用户查询、资料管理、关注关系

### 5.2 拆分前必须做的事

- [ ] 将 `UserMapper` 的直接引用全部替换为 `UserService` 调用
- [ ] `UserProfileMapper` 引用替换为 `CommunityMemberService` 或独立 `UserProfileService`
- [ ] 为每个模块补充完整的集成测试
- [ ] 引入 API 网关（如 Spring Cloud Gateway）
- [ ] 引入服务注册中心（Nacos / Consul）

---

## 6. 开发规范

### 6.1 新增代码放置规则

| 代码类型 | 放置位置 | 示例 |
|---------|---------|------|
| 用户相关实体/查询 | `user/` | `User`, `UserMapper` |
| 内容社区功能 | `community/` 对应子包 | `community/post/` |
| 直播功能 | `live/` | `LiveRoom`, `LiveRoomService` |
| 媒体上传/存储 | `media/` | `MediaService`, `MediaFile` |
| 钱包/积分 | `wallet/` | `PigWalletService` |
| 公共工具/异常/响应 | `common/` | `ApiResponse`, `BusinessException` |
| 安全/认证 | `security/` | `JwtService`, `SecurityConfig` |
| 基础设施 | `infrastructure/` | `RedisCacheService`, `RateLimitInterceptor` |
| Spring 配置 | `config/` | `WebMvcConfig`, `WebSocketConfig` |

### 6.2 禁止事项

- ❌ 禁止在非 `user` 模块中直接注入 `UserMapper`（应通过 `UserService`）
- ❌ 禁止在非 `community.member` 模块中直接注入 `UserProfileMapper`
- ❌ 禁止跨模块直接调用 Mapper（必须通过 Service 层）
- ❌ 禁止循环依赖（A → B → A）

---

## 7. 已清理的死包

以下包在阶段 9.1 中已删除（仅剩空 dto 子包，无任何引用）：

- `daily/` — 早期日报模块
- `github/` — GitHub 集成（未实现）
- `note/` — 笔记模块
- `portfolio/` — 作品集模块
- `project/` — 项目管理模块
- `statistics/` — 统计模块
- `tag/` — 标签模块
- `task/` — 任务模块

这些模块对应的 Flyway 迁移已在 V8 中通过 `DROP TABLE IF EXISTS` 清理。
