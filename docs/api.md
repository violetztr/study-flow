# 接口文档

后端使用统一返回格式，并通过 Knife4j / Swagger 自动生成在线接口文档。

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
GET /api/portfolio/**
GET /doc.html
GET /v3/api-docs/**
```

其他业务接口都需要登录。登录成功后，请求头携带：

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

## 项目接口

```text
GET /api/projects
POST /api/projects
PUT /api/projects/{id}
DELETE /api/projects/{id}
```

项目请求：

```json
{
  "name": "DevFlow Studio",
  "description": "个人全栈研发中台",
  "status": "ACTIVE"
}
```

状态：

- `ACTIVE`：启用
- `ARCHIVED`：归档

## 项目中台接口

### 项目档案

```text
GET /api/projects/{id}/profile
PUT /api/projects/{id}/profile
```

请求：

```json
{
  "headline": "个人全栈研发中台",
  "productionUrl": "https://www.violet-surf.com",
  "apiDocUrl": "https://www.violet-surf.com/doc.html",
  "databaseDocUrl": "https://github.com/violetztr/study-flow/blob/main/docs/database.md",
  "architectureSummary": "React + Spring Boot + MySQL + Docker",
  "interviewHighlights": "包含权限、测试、部署、GitHub 同步和公开作品集",
  "coverImageUrl": "https://example.com/cover.png"
}
```

### 技术栈

```text
PUT /api/projects/{id}/tech-stacks
```

请求：

```json
[
  { "name": "React", "category": "FRONTEND", "sortOrder": 1 },
  { "name": "Spring Boot", "category": "BACKEND", "sortOrder": 2 },
  { "name": "Docker", "category": "DEPLOYMENT", "sortOrder": 3 }
]
```

分类：

- `FRONTEND`
- `BACKEND`
- `DATABASE`
- `DEPLOYMENT`
- `TOOLING`
- `OTHER`

### GitHub 仓库

```text
PUT /api/projects/{id}/github
POST /api/projects/{id}/github/sync
```

保存仓库配置：

```json
{
  "owner": "violetztr",
  "repo": "study-flow"
}
```

同步后返回：

```json
{
  "owner": "violetztr",
  "repo": "study-flow",
  "htmlUrl": "https://github.com/violetztr/study-flow",
  "defaultBranch": "main",
  "primaryLanguage": "Java",
  "stars": 1,
  "forks": 0,
  "readmePresent": true
}
```

### 作品集发布设置

```text
PUT /api/projects/{id}/portfolio
```

请求：

```json
{
  "slug": "devflow-studio",
  "publicVisible": true,
  "featured": true,
  "displayOrder": 1,
  "publicSummary": "一个个人全栈研发中台项目。"
}
```

## 公开作品集接口

这些接口不需要登录。

```text
GET /api/portfolio/projects
GET /api/portfolio/projects/{slug}
```

返回内容会组合项目、项目档案、技术栈、GitHub 仓库和作品集发布信息。

## 任务接口

```text
GET /api/tasks
POST /api/tasks
PUT /api/tasks/{id}
DELETE /api/tasks/{id}
```

支持查询参数：

- `projectId`
- `status`
- `priority`
- `keyword`

任务状态：

- `PENDING`
- `IN_PROGRESS`
- `DONE`

任务优先级：

- `LOW`
- `MEDIUM`
- `HIGH`

## 标签接口

```text
GET /api/tags
POST /api/tags
```

## 统计接口

```text
GET /api/statistics/overview
```

## 笔记接口

```text
GET /api/notes
POST /api/notes
GET /api/notes/{id}
PUT /api/notes/{id}
DELETE /api/notes/{id}
PUT /api/notes/{id}/blocks
```

笔记块类型：

- `paragraph`
- `heading`
- `todo`
- `quote`
- `code`

## 日常接口

```text
GET /api/daily/plans?date=2026-07-09
POST /api/daily/plans
PUT /api/daily/plans/{id}
GET /api/daily/journal?date=2026-07-09
PUT /api/daily/journal
GET /api/daily/habits
POST /api/daily/habits
PUT /api/daily/habits/{id}/records
```

## Violet Circle 社区接口

社区接口都需要登录。普通成员可以查看动态、话题、成员，发布帖子、评论和点赞；管理员或圈主可以使用 `/api/admin/community/**` 管理内容和成员。

### 话题和动态流

```text
GET /api/community/topics
GET /api/community/feed
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
  "title": "今天的学习进展",
  "content": "完成了社区模块的后端接口。"
}
```

规则：

- 只能编辑和删除自己的帖子。
- 删除是软删除，帖子状态变为 `DELETED`。
- 禁言成员不能发帖。

### 评论

```text
GET /api/community/posts/{postId}/comments
POST /api/community/posts/{postId}/comments
DELETE /api/community/comments/{commentId}
```

评论请求：

```json
{
  "content": "这个思路不错。"
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
  "bio": "正在学习 Java 全栈和 React。",
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
- 被禁言成员仍可浏览内容，但不能发帖、评论、点赞或取消点赞。
- 被禁用用户不能登录，已有 Token 也不能继续访问业务接口。

## 常见错误

- `401`：未登录或 Token 无效
- `400`：请求参数错误或业务规则不满足
- `404`：访问的数据不存在，或不属于当前用户
- `500`：服务器内部错误
