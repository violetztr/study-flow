# StudyFlow V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个可上线、可写进简历的学习任务管理系统 V1，包含 Java Spring Boot 后端、React TypeScript 前端、MySQL/Redis、基础测试、接口文档、README、数据库文档和部署文档。

**Architecture:** 项目采用前后端分离架构。后端使用 Spring Boot 提供 REST API，MySQL 保存业务数据，Redis 预留登录相关能力；前端使用 React + Vite 构建后台管理式页面；Docker Compose 负责本地和服务器部署。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Redis, JWT, Knife4j, JUnit, React, TypeScript, Vite, Ant Design, React Router, TanStack Query, Axios, Docker, Nginx.

---

## 执行原则

- 每完成一个小任务就运行对应验证命令。
- 每完成一个稳定阶段就提交一次 Git。
- 后端优先完成 API 和测试，再做前端联调。
- 文档跟着功能一起更新，不等最后补。
- 所有文档使用中文。

## 目标目录结构

```text
study-flow/
  backend/
    pom.xml
    src/main/java/com/studyflow/
    src/main/resources/
    src/test/java/com/studyflow/
  frontend/
    package.json
    src/
  docs/
    database.md
    api.md
    deploy.md
    superpowers/
      specs/
      plans/
  docker-compose.yml
  README.md
```

## 阶段 1：项目基础和仓库说明

### Task 1: 创建根目录基础文件

**Files:**
- Create: `README.md`
- Create: `.gitignore`
- Create: `docs/database.md`
- Create: `docs/api.md`
- Create: `docs/deploy.md`

- [ ] **Step 1: 创建根 README 初版**

写入 `README.md`：

```markdown
# StudyFlow 学习任务管理系统

StudyFlow 是一个 Java 全栈学习项目，用来管理个人学习项目、任务、标签和进度统计。

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL、Redis、JWT、Knife4j、JUnit
- 前端：React、TypeScript、Vite、Ant Design、React Router、TanStack Query、Axios
- 部署：Docker、Docker Compose、Nginx、Linux 云服务器

## 核心功能

- 用户注册和登录
- 项目管理
- 任务管理
- 标签管理
- 任务筛选
- 学习进度统计
- 接口文档
- Docker 部署

## 本地开发

后端、前端、数据库和部署说明会随着项目开发逐步完善。
```

- [ ] **Step 2: 创建 Git 忽略规则**

写入 `.gitignore`：

```gitignore
.idea/
.vscode/
target/
node_modules/
dist/
build/
.env
.env.local
*.log
```

- [ ] **Step 3: 创建文档骨架**

写入 `docs/database.md`：

```markdown
# 数据库设计

本文档记录 StudyFlow V1 的数据表、字段、关系和索引。
```

写入 `docs/api.md`：

```markdown
# 接口文档

后端启动后，Knife4j 接口文档地址为：

```text
http://localhost:8080/doc.html
```
```

写入 `docs/deploy.md`：

```markdown
# 部署说明

本文档记录 StudyFlow 使用 Docker Compose 部署到 Linux 云服务器的步骤。
```

- [ ] **Step 4: 验证文件存在**

Run: `Get-ChildItem -Force`

Expected: 能看到 `README.md`、`.gitignore`、`docs`。

- [ ] **Step 5: 提交**

```powershell
git add README.md .gitignore docs/database.md docs/api.md docs/deploy.md
git commit -m "docs: add project documentation skeleton"
```

## 阶段 2：后端工程骨架

### Task 2: 创建 Spring Boot 后端项目

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/studyflow/StudyFlowApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/test/java/com/studyflow/StudyFlowApplicationTests.java`

- [ ] **Step 1: 创建 Maven 项目结构**

```powershell
New-Item -ItemType Directory -Force -Path backend/src/main/java/com/studyflow
New-Item -ItemType Directory -Force -Path backend/src/main/resources/db/migration
New-Item -ItemType Directory -Force -Path backend/src/test/java/com/studyflow
```

- [ ] **Step 2: 写入 Maven 配置**

`backend/pom.xml` 使用 Spring Boot 3、Java 17，并加入 Web、Validation、Security、MyBatis-Plus、MySQL、Redis、JWT、Knife4j、Flyway、Test 依赖。

关键依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.5.0</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

- [ ] **Step 3: 创建启动类**

`backend/src/main/java/com/studyflow/StudyFlowApplication.java`：

```java
package com.studyflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudyFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyFlowApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建配置文件**

`backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: study-flow
  datasource:
    url: jdbc:mysql://localhost:3306/study_flow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

study-flow:
  jwt:
    secret: study-flow-local-development-secret-key-change-before-production
    expiration-minutes: 1440
```

- [ ] **Step 5: 创建启动测试**

`backend/src/test/java/com/studyflow/StudyFlowApplicationTests.java`：

```java
package com.studyflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StudyFlowApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: 运行后端测试**

Run: `cd backend; mvn test`

Expected: Maven 能编译项目；如果本机没有 MySQL，启动上下文测试可能因为数据库连接失败，下一阶段先用 Docker 启动 MySQL 后再验证。

- [ ] **Step 7: 提交**

```powershell
git add backend
git commit -m "feat: add Spring Boot backend skeleton"
```

## 阶段 3：数据库和通用后端能力

### Task 3: 创建数据库表和通用返回结构

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/java/com/studyflow/common/ApiResponse.java`
- Create: `backend/src/main/java/com/studyflow/common/BusinessException.java`
- Create: `backend/src/main/java/com/studyflow/common/GlobalExceptionHandler.java`
- Modify: `docs/database.md`

- [ ] **Step 1: 写入建表 SQL**

`backend/src/main/resources/db/migration/V1__init_schema.sql`：

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_projects_user_id (user_id)
);

CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    deadline DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tasks_user_id (user_id),
    INDEX idx_tasks_project_id (project_id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_priority (priority)
);

CREATE TABLE tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#1677ff',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tags_user_name (user_id, name),
    INDEX idx_tags_user_id (user_id)
);

CREATE TABLE task_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_tags_task_tag (task_id, tag_id),
    INDEX idx_task_tags_task_id (task_id),
    INDEX idx_task_tags_tag_id (tag_id)
);
```

- [ ] **Step 2: 创建统一返回类**

`backend/src/main/java/com/studyflow/common/ApiResponse.java`：

```java
package com.studyflow.common;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null);
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 3: 创建业务异常**

`backend/src/main/java/com/studyflow/common/BusinessException.java`：

```java
package com.studyflow.common;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

- [ ] **Step 4: 创建统一异常处理**

`backend/src/main/java/com/studyflow/common/GlobalExceptionHandler.java`：

```java
package com.studyflow.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("参数错误");
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

- [ ] **Step 5: 更新数据库文档**

在 `docs/database.md` 写入 5 张表的用途、字段和索引，内容与 SQL 保持一致。

- [ ] **Step 6: 验证**

Run: `cd backend; mvn test`

Expected: 编译通过。

- [ ] **Step 7: 提交**

```powershell
git add backend docs/database.md
git commit -m "feat: add database schema and common API response"
```

## 阶段 4：用户注册登录和 JWT 鉴权

### Task 4: 实现用户注册、登录、当前用户接口

**Files:**
- Create: `backend/src/main/java/com/studyflow/user/User.java`
- Create: `backend/src/main/java/com/studyflow/user/UserMapper.java`
- Create: `backend/src/main/java/com/studyflow/auth/AuthController.java`
- Create: `backend/src/main/java/com/studyflow/auth/AuthService.java`
- Create: `backend/src/main/java/com/studyflow/auth/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/studyflow/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/studyflow/auth/dto/LoginResponse.java`
- Create: `backend/src/main/java/com/studyflow/security/JwtService.java`
- Create: `backend/src/main/java/com/studyflow/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/studyflow/auth/AuthControllerTest.java`

- [ ] **Step 1: 写注册成功测试**

测试目标：`POST /api/auth/register` 返回成功，并且响应里包含用户基础信息。

- [ ] **Step 2: 写重复用户名测试**

测试目标：重复注册同一个用户名时返回业务错误。

- [ ] **Step 3: 实现用户实体和 Mapper**

`User` 对应 `users` 表，字段为 `id`、`username`、`email`、`passwordHash`、`createdAt`、`updatedAt`。

- [ ] **Step 4: 实现注册逻辑**

注册逻辑：

1. 检查用户名是否存在。
2. 检查邮箱是否存在。
3. 使用 `BCryptPasswordEncoder` 加密密码。
4. 保存用户。
5. 返回用户 ID、用户名、邮箱。

- [ ] **Step 5: 写登录成功和错误密码测试**

测试目标：

- 正确账号密码返回 JWT。
- 错误密码返回业务错误。

- [ ] **Step 6: 实现 JWT 服务**

`JwtService` 负责：

- 根据用户 ID 和用户名生成 Token。
- 解析 Token。
- 判断 Token 是否有效。

- [ ] **Step 7: 实现登录接口**

登录逻辑：

1. 根据用户名查用户。
2. 使用 `BCryptPasswordEncoder` 校验密码。
3. 生成 JWT。
4. 返回 Token 和用户基础信息。

- [ ] **Step 8: 实现 Security 配置**

规则：

- `/api/auth/register` 和 `/api/auth/login` 允许匿名访问。
- `/doc.html`、`/webjars/**`、`/v3/api-docs/**` 允许匿名访问。
- 其他 `/api/**` 需要登录。

- [ ] **Step 9: 实现 `GET /api/users/me`**

从 JWT 中解析当前用户，返回用户 ID、用户名、邮箱。

- [ ] **Step 10: 运行测试**

Run: `cd backend; mvn test`

Expected: 注册、登录、当前用户相关测试通过。

- [ ] **Step 11: 提交**

```powershell
git add backend
git commit -m "feat: add authentication APIs"
```

## 阶段 5：项目管理后端

### Task 5: 实现项目 CRUD

**Files:**
- Create: `backend/src/main/java/com/studyflow/project/Project.java`
- Create: `backend/src/main/java/com/studyflow/project/ProjectMapper.java`
- Create: `backend/src/main/java/com/studyflow/project/ProjectService.java`
- Create: `backend/src/main/java/com/studyflow/project/ProjectController.java`
- Create: `backend/src/main/java/com/studyflow/project/dto/ProjectRequest.java`
- Create: `backend/src/main/java/com/studyflow/project/dto/ProjectResponse.java`
- Create: `backend/src/test/java/com/studyflow/project/ProjectControllerTest.java`

- [ ] **Step 1: 写创建项目测试**

测试目标：登录用户可以创建项目，返回项目 ID、名称、描述、状态。

- [ ] **Step 2: 写项目隔离测试**

测试目标：用户 A 查询项目列表时，看不到用户 B 的项目。

- [ ] **Step 3: 实现 Project 实体、Mapper、DTO**

项目状态第一版使用字符串：

```text
ACTIVE
ARCHIVED
```

- [ ] **Step 4: 实现项目 Service**

Service 方法：

- `createProject(Long userId, ProjectRequest request)`
- `listProjects(Long userId)`
- `updateProject(Long userId, Long projectId, ProjectRequest request)`
- `deleteProject(Long userId, Long projectId)`

- [ ] **Step 5: 实现 Controller**

接口：

```text
GET    /api/projects
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

- [ ] **Step 6: 运行测试**

Run: `cd backend; mvn test`

Expected: 项目管理测试通过。

- [ ] **Step 7: 提交**

```powershell
git add backend
git commit -m "feat: add project management APIs"
```

## 阶段 6：任务、标签和统计后端

### Task 6: 实现任务、标签、统计接口

**Files:**
- Create: `backend/src/main/java/com/studyflow/task/Task.java`
- Create: `backend/src/main/java/com/studyflow/task/TaskMapper.java`
- Create: `backend/src/main/java/com/studyflow/task/TaskService.java`
- Create: `backend/src/main/java/com/studyflow/task/TaskController.java`
- Create: `backend/src/main/java/com/studyflow/tag/Tag.java`
- Create: `backend/src/main/java/com/studyflow/tag/TagMapper.java`
- Create: `backend/src/main/java/com/studyflow/tag/TagService.java`
- Create: `backend/src/main/java/com/studyflow/tag/TagController.java`
- Create: `backend/src/main/java/com/studyflow/statistics/StatisticsController.java`
- Create: `backend/src/main/java/com/studyflow/statistics/StatisticsService.java`
- Create: `backend/src/test/java/com/studyflow/task/TaskControllerTest.java`
- Create: `backend/src/test/java/com/studyflow/statistics/StatisticsControllerTest.java`

- [ ] **Step 1: 写创建任务测试**

测试目标：登录用户可以在自己的项目下创建任务。

- [ ] **Step 2: 写任务筛选测试**

测试目标：按 `status`、`projectId` 查询任务，返回正确结果。

- [ ] **Step 3: 实现任务模块**

任务状态：

```text
PENDING
IN_PROGRESS
DONE
```

任务优先级：

```text
LOW
MEDIUM
HIGH
```

接口：

```text
GET    /api/tasks
POST   /api/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
```

- [ ] **Step 4: 实现标签模块**

接口：

```text
GET  /api/tags
POST /api/tags
```

第一版创建任务时可以接收 `tagIds`，后端写入 `task_tags`。

- [ ] **Step 5: 写统计接口测试**

测试目标：统计接口只统计当前用户的数据。

- [ ] **Step 6: 实现统计接口**

`GET /api/statistics/overview` 返回：

```json
{
  "totalTasks": 10,
  "completedTasks": 4,
  "inProgressTasks": 3,
  "overdueTasks": 1
}
```

- [ ] **Step 7: 运行测试**

Run: `cd backend; mvn test`

Expected: 任务、标签、统计测试通过。

- [ ] **Step 8: 提交**

```powershell
git add backend
git commit -m "feat: add task tag and statistics APIs"
```

## 阶段 7：接口文档

### Task 7: 接入 Knife4j 并更新接口说明

**Files:**
- Create: `backend/src/main/java/com/studyflow/config/OpenApiConfig.java`
- Modify: `docs/api.md`

- [ ] **Step 1: 创建 OpenAPI 配置**

配置接口标题为 `StudyFlow API`，描述为 `StudyFlow 学习任务管理系统接口文档`。

- [ ] **Step 2: 启动后端并访问接口文档**

Run: `cd backend; mvn spring-boot:run`

Expected: 浏览器访问 `http://localhost:8080/doc.html` 能看到接口文档页面。

- [ ] **Step 3: 更新接口文档**

`docs/api.md` 写入：

- 接口文档访问地址
- 登录注册接口
- 项目接口
- 任务接口
- 标签接口
- 统计接口
- JWT 使用方式：`Authorization: Bearer <token>`

- [ ] **Step 4: 提交**

```powershell
git add backend docs/api.md
git commit -m "docs: add API documentation"
```

## 阶段 8：前端工程骨架

### Task 8: 创建 React 前端项目

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/routes/ProtectedRoute.tsx`
- Create: `frontend/src/pages/LoginPage.tsx`
- Create: `frontend/src/pages/RegisterPage.tsx`
- Create: `frontend/src/pages/DashboardPage.tsx`

- [ ] **Step 1: 创建 Vite React 项目**

Run:

```powershell
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install antd @ant-design/icons axios @tanstack/react-query react-router-dom
```

Expected: `frontend` 目录生成 React + TypeScript 项目。

- [ ] **Step 2: 配置 Axios**

`frontend/src/api/http.ts` 负责：

- 创建 Axios 实例。
- baseURL 设置为 `/api`。
- 请求拦截器自动添加 `Authorization`。
- 响应拦截器统一取 `data`。

- [ ] **Step 3: 配置 React Router**

路由：

```text
/login
/register
/dashboard
/projects
/projects/:id
/tasks
/settings/profile
```

- [ ] **Step 4: 实现登录页**

登录页字段：

- 用户名
- 密码

登录成功后：

- 保存 Token。
- 跳转 `/dashboard`。

- [ ] **Step 5: 实现注册页**

注册页字段：

- 用户名
- 邮箱
- 密码

注册成功后跳转 `/login`。

- [ ] **Step 6: 实现基础 Dashboard**

先显示统计卡片布局，数据从 `/api/statistics/overview` 获取。

- [ ] **Step 7: 运行前端**

Run: `cd frontend; npm run dev`

Expected: 浏览器访问 Vite 地址能打开页面。

- [ ] **Step 8: 提交**

```powershell
git add frontend
git commit -m "feat: add React frontend skeleton"
```

## 阶段 9：前端项目、任务、标签页面

### Task 9: 实现核心业务页面

**Files:**
- Create: `frontend/src/layouts/AppLayout.tsx`
- Create: `frontend/src/pages/ProjectsPage.tsx`
- Create: `frontend/src/pages/ProjectDetailPage.tsx`
- Create: `frontend/src/pages/TasksPage.tsx`
- Create: `frontend/src/pages/ProfilePage.tsx`
- Create: `frontend/src/api/projects.ts`
- Create: `frontend/src/api/tasks.ts`
- Create: `frontend/src/api/tags.ts`
- Create: `frontend/src/api/statistics.ts`

- [ ] **Step 1: 实现后台主布局**

布局包含：

- 左侧菜单
- 顶部当前用户区域
- 主内容区

- [ ] **Step 2: 实现项目列表页**

功能：

- 查询项目列表
- 新增项目
- 编辑项目
- 删除项目

- [ ] **Step 3: 实现任务列表页**

功能：

- 查询任务列表
- 新增任务
- 编辑任务
- 删除任务
- 按状态筛选
- 按优先级筛选
- 按项目筛选
- 按关键词搜索

- [ ] **Step 4: 实现项目详情页**

功能：

- 展示项目基础信息
- 展示当前项目下任务

- [ ] **Step 5: 实现个人信息页**

功能：

- 调用 `/api/users/me`
- 展示用户名和邮箱

- [ ] **Step 6: 前后端联调**

Run:

```powershell
cd backend
mvn spring-boot:run
```

Run:

```powershell
cd frontend
npm run dev
```

Expected: 可以从前端完成注册、登录、创建项目、创建任务、筛选任务、查看统计。

- [ ] **Step 7: 提交**

```powershell
git add frontend
git commit -m "feat: add core frontend pages"
```

## 阶段 10：Docker 和部署

### Task 10: 添加 Docker Compose 部署能力

**Files:**
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`
- Create: `docker-compose.yml`
- Modify: `docs/deploy.md`
- Modify: `README.md`

- [ ] **Step 1: 创建后端 Dockerfile**

构建 Spring Boot jar，并用 JRE 镜像运行。

- [ ] **Step 2: 创建前端 Dockerfile**

构建 React 静态文件，并用 Nginx 服务。

- [ ] **Step 3: 创建 Nginx 配置**

规则：

- `/` 指向前端静态文件。
- `/api/` 代理到后端 `http://backend:8080`。

- [ ] **Step 4: 创建 docker-compose.yml**

服务：

- `mysql`
- `redis`
- `backend`
- `frontend`

端口：

- 前端 `80:80`
- 后端内部 `8080`
- MySQL 本地可选 `3306:3306`
- Redis 本地可选 `6379:6379`

- [ ] **Step 5: 本地验证 Docker Compose**

Run: `docker compose up --build`

Expected: 浏览器访问 `http://localhost` 能打开前端，前端能调用 `/api`。

- [ ] **Step 6: 更新部署文档**

`docs/deploy.md` 写入：

- 服务器安装 Docker
- 上传代码或拉取 GitHub 仓库
- 配置环境变量
- 执行 `docker compose up -d --build`
- 查看日志
- 更新版本

- [ ] **Step 7: 提交**

```powershell
git add backend/Dockerfile frontend/Dockerfile frontend/nginx.conf docker-compose.yml docs/deploy.md README.md
git commit -m "feat: add Docker deployment"
```

## 阶段 11：最终文档和上线收尾

### Task 11: 完成 README、GitHub 和上线地址

**Files:**
- Modify: `README.md`
- Modify: `docs/database.md`
- Modify: `docs/api.md`
- Modify: `docs/deploy.md`

- [ ] **Step 1: 完善 README**

README 最终包含：

- 项目介绍
- 技术栈
- 功能清单
- 项目截图
- 本地启动步骤
- 测试命令
- Docker 启动步骤
- 接口文档地址
- 线上访问地址

- [ ] **Step 2: 完善数据库文档**

确认 `docs/database.md` 与最终 SQL 一致。

- [ ] **Step 3: 完善接口文档**

确认 `docs/api.md` 与最终 Controller 接口一致。

- [ ] **Step 4: 完善部署文档**

确认 `docs/deploy.md` 包含真实服务器部署步骤。

- [ ] **Step 5: 创建 GitHub 仓库**

在 GitHub 创建公开仓库 `study-flow`，然后执行：

```powershell
git remote add origin https://github.com/<你的GitHub用户名>/study-flow.git
git branch -M main
git push -u origin main
```

- [ ] **Step 6: 上线部署**

在云服务器拉取仓库并执行：

```bash
docker compose up -d --build
```

- [ ] **Step 7: 最终验证**

验证清单：

- 能访问线上前端。
- 能注册用户。
- 能登录。
- 能创建项目。
- 能创建任务。
- 能筛选任务。
- 能查看统计。
- 能访问接口文档。

- [ ] **Step 8: 最终提交**

```powershell
git add README.md docs/database.md docs/api.md docs/deploy.md
git commit -m "docs: finalize StudyFlow project documentation"
```

## 推荐学习顺序

如果你每天学习 2 小时，可以这样安排：

- 第 1 天：根目录文档和后端骨架
- 第 2 天：数据库表和通用返回结构
- 第 3 天：注册登录和 JWT
- 第 4 天：项目管理接口
- 第 5 天：任务和标签接口
- 第 6 天：统计接口和 Knife4j
- 第 7 天：React 项目骨架、登录注册页面
- 第 8 天：Dashboard、项目页面
- 第 9 天：任务页面、筛选、联调
- 第 10 天：Docker、README、部署文档
- 第 11 天：GitHub 仓库和云服务器部署
- 第 12 天：整理截图、复盘、准备简历描述

## 简历描述参考

```text
StudyFlow 学习任务管理系统

项目描述：基于 Spring Boot 和 React 实现的个人学习任务管理系统，支持用户注册登录、项目管理、任务管理、标签管理、任务筛选、进度统计和 Docker 部署。

技术栈：Spring Boot、MyBatis-Plus、MySQL、Redis、JWT、Knife4j、JUnit、React、TypeScript、Vite、Ant Design、Docker、Nginx。

个人职责：
1. 设计用户、项目、任务、标签等核心数据表。
2. 实现 JWT 登录鉴权和统一接口返回格式。
3. 实现项目、任务、标签、统计等 REST API。
4. 使用 React + TypeScript 构建前端页面并完成前后端联调。
5. 编写基础接口测试、接口文档、数据库文档和 Docker 部署文档。
```

## 自检结果

- 规格文档中的注册登录、项目、任务、标签、筛选、统计、接口文档、测试、README、数据库文档、部署文档、Docker 部署要求都已覆盖。
- 计划按后端优先、前端联调、部署收尾的顺序组织，便于逐步验证。
- 关键命令、目录和模块边界已经固定。
