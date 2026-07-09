# Violet Circle 朋友小圈子社区设计规格

## 背景

当前项目已经从 StudyFlow 学习任务管理系统升级到 DevFlow Studio 个人全栈研发中台，具备登录注册、学习项目、任务、笔记、日常计划、项目中台、GitHub 同步和公开作品集等能力。

新的方向是把现有网站改造成一个给自己朋友使用的小圈子社区。这个社区不使用邀请码，也不做注册审核。用户注册后可以直接进入社区。后续再逐步加强隐私、权限和加密能力。

## 产品定位

产品名：Violet Circle。

一句话定位：一个开放注册、面向熟人朋友的小圈子社区，用来发动态、评论互动、分享资源、记录生活和学习。

产品重心从“个人学习管理工具”切换为“朋友社区”。原来的学习、笔记、日常、项目中台能力不删除，作为社区里的个人工具箱保留。

## 用户范围

第一版默认所有注册用户都能进入社区。

用户类型：

- 普通用户：注册、登录、发帖、评论、点赞、查看成员、编辑自己的资料。
- 管理员：管理帖子、评论、话题和用户状态。

第一版不做邀请码、不做注册审核、不做好友可见范围。后续可以增加好友关系、黑名单、内容可见范围和更严格的隐私控制。

## 第一版功能范围

### 1. 社区首页动态流

登录后默认进入社区首页，而不是原来的学习驾驶舱。

动态流展示：

- 帖子作者
- 作者头像或首字母头像
- 发布时间
- 话题标签
- 标题
- 正文摘要
- 点赞数
- 评论数

排序规则：第一版按创建时间倒序。

### 2. 发帖

用户可以发布帖子。

帖子字段：

- 标题
- 正文
- 话题
- 类型：生活、学习、资源、游戏、约局、吐槽、项目

第一版只支持文本和链接，不做图片上传。图片上传放到第二阶段。

### 3. 帖子详情

帖子详情展示完整正文、点赞状态、评论列表。

用户可以：

- 点赞或取消点赞
- 发布评论
- 删除自己的帖子
- 删除自己的评论

管理员可以删除任意帖子和评论。

### 4. 评论

第一版只做一级评论，不做楼中楼。

评论字段：

- 帖子 ID
- 评论用户
- 评论内容
- 创建时间
- 删除状态

### 5. 点赞

用户可以对帖子点赞。

规则：

- 同一个用户对同一个帖子只能点赞一次。
- 再点一次表示取消点赞。

第一版只支持帖子点赞，不支持评论点赞。

### 6. 话题

系统内置话题：

- 生活
- 学习
- 资源
- 游戏
- 约局
- 吐槽
- 项目

管理员可以新增、编辑、禁用话题。

### 7. 成员列表和个人主页

成员列表展示：

- 用户名
- 昵称
- 简介
- 加入时间
- 发帖数

个人主页展示：

- 用户基础资料
- 个人简介
- 最近发布的帖子

第一版不做关注关系。

### 8. 管理员后台

管理员后台第一版包含：

- 用户列表
- 禁用或启用用户
- 帖子管理
- 评论管理
- 话题管理

管理员能力先做基础版，不做复杂审核流。

## 暂不做的功能

第一版明确不做：

- 图片上传
- 私信
- 实时聊天
- 关注关系
- 多个圈子
- 推荐算法
- 端到端加密
- 移动端 App

这些功能放到后续阶段。第一版目标是先让社区闭环跑起来。

## 原有功能如何处理

不删除现有功能。

调整方式：

- 原 `/dashboard` 学习驾驶舱改为次要入口。
- 新增 `/circle` 作为登录后的主入口。
- 学习项目、任务、笔记、日常、项目中台归入“个人工具箱”菜单。
- 公开作品集继续保留，用于展示项目能力。

这样既能保留已经完成的工程成果，也能让产品主线变成社区。

## 页面设计

登录后主要页面：

```text
/circle                  社区动态流
/circle/posts/new        发帖
/circle/posts/:id        帖子详情
/circle/topics           话题列表
/circle/members          成员列表
/circle/members/:id      个人主页
/admin/community         社区管理后台
```

保留原有页面：

```text
/projects
/tasks
/notes
/daily
/project-hub
/portfolio
```

菜单结构建议：

```text
Violet Circle
  社区首页
  发动态
  话题
  成员

个人工具箱
  学习项目
  学习任务
  笔记
  日常计划
  项目中台

管理
  社区后台
  个人资料
```

## 后端模块设计

新增后端包：

```text
com.studyflow.community
```

建议拆分：

```text
community/post
community/comment
community/reaction
community/topic
community/member
community/admin
```

第一版可以保持模块化单体，不拆微服务。等社区规模变大后，再考虑拆成独立服务。

## 数据库设计

新增表：

### community_topics

保存社区话题。

核心字段：

- `id`
- `name`
- `description`
- `color`
- `active`
- `sort_order`
- `created_at`
- `updated_at`

### community_posts

保存帖子。

核心字段：

- `id`
- `user_id`
- `topic_id`
- `title`
- `content`
- `post_type`
- `pinned`
- `deleted`
- `created_at`
- `updated_at`

索引：

- `idx_community_posts_user_id`
- `idx_community_posts_topic_id`
- `idx_community_posts_created_at`

### community_comments

保存评论。

核心字段：

- `id`
- `post_id`
- `user_id`
- `content`
- `deleted`
- `created_at`
- `updated_at`

索引：

- `idx_community_comments_post_id`
- `idx_community_comments_user_id`

### community_post_reactions

保存帖子点赞。

核心字段：

- `id`
- `post_id`
- `user_id`
- `reaction_type`
- `created_at`

约束：

- 同一个 `post_id + user_id + reaction_type` 唯一。

### user_profiles

如果现有用户资料不足，新增用户展示资料表。

核心字段：

- `id`
- `user_id`
- `nickname`
- `bio`
- `avatar_url`
- `created_at`
- `updated_at`

### user_roles

第一版可以先在用户表增加 `role` 字段，也可以新增角色表。为了实现简单，第一版建议给 `users` 表增加：

- `role`：`USER` 或 `ADMIN`
- `status`：`ACTIVE` 或 `DISABLED`

## 接口设计

社区公开给登录用户的接口：

```text
GET /api/community/posts
POST /api/community/posts
GET /api/community/posts/{id}
PUT /api/community/posts/{id}
DELETE /api/community/posts/{id}

GET /api/community/posts/{id}/comments
POST /api/community/posts/{id}/comments
DELETE /api/community/comments/{id}

PUT /api/community/posts/{id}/like
DELETE /api/community/posts/{id}/like

GET /api/community/topics
GET /api/community/members
GET /api/community/members/{id}
```

管理员接口：

```text
GET /api/admin/community/users
PUT /api/admin/community/users/{id}/status
GET /api/admin/community/posts
DELETE /api/admin/community/posts/{id}
GET /api/admin/community/comments
DELETE /api/admin/community/comments/{id}
POST /api/admin/community/topics
PUT /api/admin/community/topics/{id}
```

## 权限和安全

第一版必须做到：

- 密码使用 BCrypt 保存。
- 登录使用 JWT。
- 所有社区接口必须登录。
- 用户只能修改或删除自己的帖子和评论。
- 管理员可以管理所有帖子和评论。
- 被禁用用户不能登录或不能访问社区接口。
- 删除帖子和评论优先使用软删除。

后续安全升级：

- 敏感字段数据库加密。
- 操作日志。
- 登录设备管理。
- 接口限流。
- 举报和审核。
- 私信端到端加密。

重要说明：HTTPS、密码哈希、JWT 鉴权、数据库字段加密、端到端加密是不同层级的安全能力，不能混为一谈。第一版先保证基础安全和权限正确，后续再做更强加密。

## 前端设计

前端新增：

```text
frontend/src/api/community.ts
frontend/src/pages/CircleFeedPage.tsx
frontend/src/pages/CreatePostPage.tsx
frontend/src/pages/PostDetailPage.tsx
frontend/src/pages/TopicsPage.tsx
frontend/src/pages/MembersPage.tsx
frontend/src/pages/MemberProfilePage.tsx
frontend/src/pages/CommunityAdminPage.tsx
frontend/src/components/community/PostCard.tsx
frontend/src/components/community/PostEditor.tsx
frontend/src/components/community/CommentList.tsx
frontend/src/components/community/TopicBadge.tsx
```

视觉方向：

- 不做传统论坛的老旧样式。
- 更像私密朋友动态流。
- 卡片式信息流。
- 温暖但不幼稚。
- 移动端优先保证可读。

## 测试策略

后端测试：

- 注册登录后可以发布帖子。
- 未登录不能访问社区接口。
- 用户可以评论帖子。
- 用户可以点赞和取消点赞。
- 用户不能删除别人的帖子。
- 管理员可以删除任意帖子。
- 禁用用户不能访问社区。

前端验证：

- `npm run build` 必须通过。
- 社区页面路由必须能编译。
- 动态流、发帖、详情、成员页、后台页的 API 类型必须正确。

## 第一阶段验收标准

第一阶段完成后，用户应该可以：

- 注册并登录。
- 进入 `/circle` 社区首页。
- 发布一条动态。
- 在动态列表看到它。
- 进入详情页。
- 评论它。
- 点赞或取消点赞。
- 查看话题和成员列表。
- 管理员可以进入后台管理帖子、评论、话题和用户状态。

## 后续阶段

第二阶段：

- 图片上传。
- 通知。
- 举报。
- 用户头像。
- 个人主页美化。

第三阶段：

- 好友关系。
- 私信。
- 私信端到端加密。
- 内容可见范围。
- 防刷限流。

## 迁移策略

第一阶段不删除任何旧表和旧页面。

改造顺序：

1. 新增社区数据库表。
2. 新增社区后端接口。
3. 新增社区前端页面。
4. 修改登录后的默认首页到 `/circle`。
5. 调整菜单，把旧学习功能放入个人工具箱。
6. 更新 README、API、数据库和部署文档。

这样可以保证每一步都能测试和回滚，不会一次性把原网站改坏。
