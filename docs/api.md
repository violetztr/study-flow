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

## 常见错误

- `401`：未登录或 Token 无效
- `400`：请求参数错误或业务规则不满足
- `404`：访问的数据不存在，或不属于当前用户
- `500`：服务器内部错误
