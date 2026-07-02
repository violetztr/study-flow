# StudyFlow 项目设计文档

## 项目目标

StudyFlow 是一个可以放进简历的学习任务管理系统 / 个人项目管理系统。这个项目的目标不是只做一个简单 demo，而是完整展示 Java 全栈开发流程：React 页面调用 Spring Boot 接口，Spring Boot 操作 MySQL 数据库，Redis 支持登录相关能力，Docker 负责部署，GitHub 仓库提供清晰文档。

做完第一版后，这个项目应该具备以下成果：

- 有 GitHub 仓库
- 有线上访问地址
- 有 README 项目说明
- 有数据库设计文档
- 有接口文档
- 有基础后端测试
- 有部署说明

## 目标用户

第一版主要面向个人学习者。用户可以创建学习项目，把项目拆成任务，给任务设置状态、优先级和截止时间，并查看自己的学习进度统计。

## 技术栈

### 前端

- React
- TypeScript
- Vite
- Ant Design
- React Router
- TanStack Query
- Axios

### 后端

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- JWT 登录鉴权
- Knife4j 或 Swagger 接口文档
- JUnit 后端测试

### 部署

- Docker
- Docker Compose
- Nginx
- Linux 云服务器

## 第一版功能范围

第一版要做完整，但不要做太大。核心功能包括：

- 用户注册
- 用户登录
- JWT 登录鉴权
- 查看当前登录用户信息
- 项目新增、列表、编辑、删除
- 任务新增、列表、编辑、删除、状态修改
- 任务字段：标题、描述、状态、优先级、截止时间、所属项目
- 标签新增
- 给任务打标签
- 按状态、优先级、项目、关键词筛选任务
- 统计概览：总任务数、已完成任务数、进行中任务数、逾期任务数
- 自动生成接口文档
- 基础后端测试
- README、数据库设计、接口说明、部署说明
- Docker 本地启动和服务器部署

第一版暂时不做团队协作、消息通知、AI、支付、复杂日历和文件附件。这些功能等核心项目完成后再作为升级版本考虑。

## 整体架构

项目采用前后端分离架构。

- React 前端负责页面展示、路由、表单、按钮交互和接口请求状态。
- Spring Boot 后端负责 REST 接口、参数校验、登录鉴权和业务逻辑。
- MySQL 保存用户、项目、任务、标签和任务标签关系。
- Redis 用于支持登录相关能力，例如后续可以做验证码、Token 黑名单等。
- Nginx 部署前端静态页面，并把 `/api` 请求转发到后端服务。
- Docker Compose 负责把 MySQL、Redis、后端、前端服务统一启动。

## 仓库结构

```text
study-flow/
  backend/
  frontend/
  docs/
    database.md
    api.md
    deploy.md
  docs/superpowers/specs/
    2026-07-03-study-flow-design.md
  docker-compose.yml
  README.md
```

## 前端设计

### 页面路由

```text
/login              登录页
/register           注册页
/dashboard          统计概览页
/projects           项目列表页
/projects/:id       项目详情页
/tasks              全部任务页
/settings/profile   个人信息页
```

### 页面说明

- 登录页：输入账号密码，登录成功后保存后端返回的 JWT。
- 注册页：创建新用户账号。
- 统计概览页：展示任务总数、完成数、进行中数量、逾期数量。
- 项目列表页：展示项目列表，支持新增、编辑、删除项目。
- 项目详情页：展示某个项目的信息和它下面的任务。
- 全部任务页：展示所有任务，支持按状态、优先级、项目、关键词筛选。
- 个人信息页：展示当前登录用户信息。

### 前端状态管理

- 接口数据用 TanStack Query 管理。
- 表单状态用 React 组件状态或 Ant Design Form 管理。
- 登录 Token 由一个简单的登录工具模块保存。
- Axios 拦截器负责给请求自动加上 Token。
- 未登录访问受保护页面时，前端跳转到 `/login`。

## 后端设计

后端采用常见 Spring Boot 分层结构。

- Controller：接收 HTTP 请求，返回接口响应。
- Service：处理业务逻辑和事务。
- Mapper：使用 MyBatis-Plus 操作数据库。
- Entity：对应数据库表。
- DTO：接收前端请求参数，返回前端需要的数据。
- Security：校验 JWT，获取当前登录用户。
- Common：统一返回格式、统一异常处理、参数校验错误处理。

## 接口返回格式

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
  "message": "参数错误",
  "data": null
}
```

## 数据库设计

### users 用户表

保存用户信息。

- id：用户 ID
- username：用户名
- email：邮箱
- password_hash：加密后的密码
- created_at：创建时间
- updated_at：更新时间

### projects 项目表

保存用户创建的学习项目。

- id：项目 ID
- user_id：所属用户 ID
- name：项目名称
- description：项目描述
- status：项目状态
- created_at：创建时间
- updated_at：更新时间

### tasks 任务表

保存项目下面的任务。

- id：任务 ID
- user_id：所属用户 ID
- project_id：所属项目 ID
- title：任务标题
- description：任务描述
- status：任务状态
- priority：任务优先级
- deadline：截止时间
- completed_at：完成时间
- created_at：创建时间
- updated_at：更新时间

### tags 标签表

保存用户自己的标签。

- id：标签 ID
- user_id：所属用户 ID
- name：标签名称
- color：标签颜色
- created_at：创建时间
- updated_at：更新时间

### task_tags 任务标签关联表

保存任务和标签之间的多对多关系。

- id：关联 ID
- task_id：任务 ID
- tag_id：标签 ID
- created_at：创建时间

## 接口设计

### 登录注册

```text
POST /api/auth/register    用户注册
POST /api/auth/login       用户登录
GET  /api/users/me         获取当前登录用户
```

### 项目管理

```text
GET    /api/projects       查询项目列表
POST   /api/projects       新增项目
PUT    /api/projects/{id}  修改项目
DELETE /api/projects/{id}  删除项目
```

### 任务管理

```text
GET    /api/tasks          查询任务列表
POST   /api/tasks          新增任务
GET    /api/tasks/{id}     查询任务详情
PUT    /api/tasks/{id}     修改任务
DELETE /api/tasks/{id}     删除任务
```

### 标签管理

```text
GET  /api/tags             查询标签列表
POST /api/tags             新增标签
```

### 统计概览

```text
GET /api/statistics/overview  查询统计概览
```

## 数据流

1. 用户在 React 登录页输入账号密码。
2. 前端调用 `POST /api/auth/login`。
3. 后端校验账号密码，成功后返回 JWT。
4. 前端保存 JWT。
5. 前端后续请求接口时自动带上 JWT。
6. 后端解析 JWT，得到当前登录用户。
7. 项目、任务、标签、统计接口只返回当前用户自己的数据。

## 错误处理

- 参数校验失败时，后端返回清楚的错误信息。
- 未登录访问接口时，后端返回 `401`，前端跳转到登录页。
- 用户访问不属于自己的数据时，后端返回 `403`。
- 查询不存在的数据时，后端返回 `404`。
- 后端出现未知异常时，返回 `500` 和安全的错误提示。
- 前端表单错误显示在对应输入框附近。
- 前端接口请求失败时，用 Ant Design 的提示组件显示错误。

## 测试策略

第一版先写后端基础测试，重点覆盖核心接口。

- 用户注册成功
- 重复用户名或邮箱注册失败
- 用户登录成功
- 错误密码登录失败
- 当前用户创建项目成功
- 项目列表只返回当前用户的数据
- 当前用户在自己的项目下创建任务成功
- 按状态和项目筛选任务成功
- 统计接口只统计当前用户的数据

第一版暂时不要求写前端自动化测试。前端功能用 README 里的手动验证步骤检查。

## 文档交付内容

### README.md

README 需要说明：

- 项目介绍
- 技术栈
- 核心功能
- 项目截图
- 本地启动方式
- 测试命令
- 部署方式简介
- 上线访问地址

### docs/database.md

数据库文档需要说明：

- 每张表的用途
- 每个字段的含义
- 表之间的关系
- 建议索引

### docs/api.md

接口文档需要说明：

- Knife4j 或 Swagger 的访问地址
- 核心接口列表
- 登录接口和鉴权方式
- 常见错误码

### docs/deploy.md

部署文档需要说明：

- 服务器环境要求
- Docker Compose 启动方式
- Nginx 反向代理配置
- 环境变量配置
- 如何更新线上版本

## 部署设计

Docker Compose 负责启动这些服务：

- MySQL
- Redis
- Spring Boot 后端
- Nginx 前端服务

部署流程：

1. 构建 React 前端。
2. 构建 Spring Boot 后端 jar 包。
3. 构建 Docker 镜像。
4. 使用 Docker Compose 启动服务。
5. 浏览器访问服务器 IP 或域名。

## 完成标准

第一版完成时，需要满足这些标准：

- GitHub 仓库包含前端、后端、文档和部署文件。
- 后端可以本地启动，并连接 MySQL 和 Redis。
- 前端可以本地启动，并调用后端接口。
- 用户可以注册、登录、创建项目、创建任务、打标签、筛选任务、查看统计。
- Knife4j 或 Swagger 可以查看接口文档。
- 后端基础测试可以通过。
- README、数据库设计、接口说明、部署说明都已完成。
- Docker Compose 可以启动可部署的应用服务。
- 项目有线上访问地址。
