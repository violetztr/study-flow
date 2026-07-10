# 接口文档

后端使用统一返回格式，并通过 Knife4j / Swagger 生成在线接口文档。

## 文档地址

```text
https://www.violet-surf.com/doc.html
http://localhost:8080/doc.html
http://localhost:8080/v3/api-docs
```

## 统一返回格式

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

## 鉴权

公开接口：

```text
POST /api/auth/register
POST /api/auth/login
GET /doc.html
GET /v3/api-docs/**
```

其他 `/api/**` 接口都需要登录。登录成功后，请求头携带：

```text
Authorization: Bearer <token>
```

## 账号接口

```text
POST /api/auth/register
POST /api/auth/login
GET /api/users/me
```

注册请求：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

登录请求：

```json
{
  "username": "alice",
  "password": "password123"
}
```

## 社区接口

社区接口都需要登录。普通成员可以查看动态、话题、成员，发布帖子、评论和点赞；管理员或圈主可以使用 `/api/admin/community/**` 管理内容和成员。

### 话题和动态流

```text
GET /api/community/topics
GET /api/community/feed
```

默认话题：

```text
announcements  公告
chat           闲聊
help           求助
share          分享
```

### 帖子

```text
POST /api/community/posts
GET /api/community/posts/{postId}
PUT /api/community/posts/{postId}
DELETE /api/community/posts/{postId}
```

发帖请求：

```json
{
  "topicId": 1,
  "title": "第一条社区帖子",
  "content": "今天开始把想法发到 Ruru 社区。"
}
```

规则：

- 只能编辑和删除自己的帖子。
- 删除是软删除，帖子状态变为 `DELETED`。
- 禁言成员不能发帖、改帖或删帖。

### 评论

```text
GET /api/community/posts/{postId}/comments
POST /api/community/posts/{postId}/comments
DELETE /api/community/comments/{commentId}
```

评论请求：

```json
{
  "content": "这个想法不错。"
}
```

规则：

- 只能删除自己的评论。
- 删除是软删除，评论状态变为 `DELETED`。
- 评论会更新帖子评论数和最近活跃时间。

### 点赞

```text
POST /api/community/posts/{postId}/reactions/like
DELETE /api/community/posts/{postId}/reactions/like
```

规则：

- 重复点赞不会重复计数。
- 重复取消点赞不会把计数减成负数。
- 禁言成员不能点赞或取消点赞。

### 成员和资料

```text
GET /api/community/members/me
PUT /api/community/members/me/profile
GET /api/community/members
GET /api/community/members/{userId}
```

资料请求：

```json
{
  "displayName": "Alice",
  "bio": "喜欢折腾全栈项目。",
  "skills": "Java,Spring Boot,React,Docker",
  "githubUrl": "https://github.com/alice",
  "websiteUrl": "https://example.com"
}
```

### 管理接口

```text
POST /api/admin/community/posts/{postId}/hide
POST /api/admin/community/posts/{postId}/restore
POST /api/admin/community/comments/{commentId}/hide
POST /api/admin/community/comments/{commentId}/restore
POST /api/admin/community/members/{userId}/mute
POST /api/admin/community/members/{userId}/unmute
```

管理请求体可以为空，也可以传入原因：

```json
{
  "reason": "内容不适合展示"
}
```

规则：

- 只有 `ADMIN` 或 `OWNER` 可以访问管理接口。
- 管理操作会写入 `community_moderation_actions`。
- 管理员不能禁言自己、圈主或同级管理员；圈主可以处理管理员和普通成员。
- 被禁言成员仍可浏览内容，但不能发帖、评论、点赞、取消点赞或修改社区资料。
- 被禁用用户不能登录，已有 Token 也不能继续访问业务接口。

## 常见错误

- `401`：未登录或 Token 无效。
- `400`：请求参数错误或业务规则不满足。
- `403`：没有权限，例如非成员访问社区、禁言成员写内容、普通成员访问管理接口。
- `404`：访问的数据不存在，或对当前用户不可见。
- `500`：服务器内部错误。
