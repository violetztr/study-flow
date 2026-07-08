# 接口文档

StudyFlow 后端使用 Knife4j / Swagger 自动生成接口文档。

## 文档访问地址

线上部署后，浏览器访问：

```text
https://www.violet-surf.com/doc.html
```

后端本地启动后，浏览器访问：

```text
http://localhost:8080/doc.html
```

OpenAPI JSON 地址：

```text
http://localhost:8080/v3/api-docs
```

## 统一返回格式

成功返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败返回：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

## 鉴权方式

除注册、登录和接口文档外，其他业务接口都需要登录。

前端登录成功后，后端会返回 JWT。后续请求需要在请求头中携带：

```text
Authorization: Bearer <token>
```

## 登录注册接口

### 用户注册

```text
POST /api/auth/register
```

请求体：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

### 用户登录

```text
POST /api/auth/login
```

请求体：

```json
{
  "username": "alice",
  "password": "password123"
}
```

返回数据包含：

- `token`：JWT 登录凭证
- `user`：当前用户基础信息

### 当前用户

```text
GET /api/users/me
```

说明：根据请求头里的 JWT 返回当前登录用户。

## 项目接口

### 查询项目列表

```text
GET /api/projects
```

### 新增项目

```text
POST /api/projects
```

请求体：

```json
{
  "name": "Java 全栈学习",
  "description": "学习 Spring Boot 和 React",
  "status": "ACTIVE"
}
```

### 修改项目

```text
PUT /api/projects/{id}
```

### 删除项目

```text
DELETE /api/projects/{id}
```

项目状态：

- `ACTIVE`：启用
- `ARCHIVED`：归档

## 任务接口

### 查询任务列表

```text
GET /api/tasks
```

支持查询参数：

- `projectId`：按项目筛选
- `status`：按任务状态筛选
- `priority`：按优先级筛选
- `keyword`：按标题关键词搜索

示例：

```text
GET /api/tasks?projectId=1&status=DONE
```

### 新增任务

```text
POST /api/tasks
```

请求体：

```json
{
  "projectId": 1,
  "title": "实现任务接口",
  "description": "完成任务新增接口",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "deadline": "2027-01-01T00:00:00",
  "estimatedMinutes": 90,
  "tagIds": [1, 2]
}
```

说明：

- `estimatedMinutes`：预计学习时长，单位分钟，可以为空；填写时必须是大于等于 0 的整数。

### 修改任务

```text
PUT /api/tasks/{id}
```

### 删除任务

```text
DELETE /api/tasks/{id}
```

任务状态：

- `PENDING`：待开始
- `IN_PROGRESS`：进行中
- `DONE`：已完成

任务优先级：

- `LOW`：低
- `MEDIUM`：中
- `HIGH`：高

## 标签接口

### 查询标签列表

```text
GET /api/tags
```

### 新增标签

```text
POST /api/tags
```

请求体：

```json
{
  "name": "后端",
  "color": "#1677ff"
}
```

## 统计接口

### 查询统计概览

```text
GET /api/statistics/overview
```

返回数据：

```json
{
  "totalTasks": 4,
  "completedTasks": 1,
  "inProgressTasks": 1,
  "overdueTasks": 1,
  "totalEstimatedMinutes": 210,
  "completedEstimatedMinutes": 120
}
```

字段说明：

- `totalEstimatedMinutes`：当前用户所有任务的预计学习总时长，单位分钟；未设置预计时长的任务按 0 计算。
- `completedEstimatedMinutes`：当前用户已完成任务的预计学习时长合计，单位分钟。

## 常见错误

- `401`：未登录或 Token 无效
- `400`：请求参数错误或业务规则不满足
- `404`：访问的数据不存在，或者不属于当前用户
- `500`：服务器内部错误
