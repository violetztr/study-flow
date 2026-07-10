# Ruru 社区

Ruru 社区是一个先从朋友小圈子开始的全栈社区项目。当前版本只保留最基础、最重要的社区能力：注册登录、动态流、发帖、评论、点赞、成员资料和基础管理。

旧的学习项目、任务、笔记、日常、项目中台和公开作品集模块已经从前后端代码中删除；数据库通过 Flyway `V8__remove_legacy_modules.sql` 删除旧业务表，只保留用户和社区相关表。

## 线上地址

- 在线访问：https://www.violet-surf.com/login
- 社区首页：https://www.violet-surf.com/circle
- 接口文档：https://www.violet-surf.com/doc.html
- GitHub 仓库：https://github.com/violetztr/study-flow

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Flyway、Knife4j、JUnit
- 前端：React、TypeScript、Vite、Ant Design、React Router、TanStack Query、Axios
- 部署：Docker、Docker Compose、Nginx、Linux 云服务器

## 核心功能

- 用户注册、登录、JWT 鉴权
- 新用户注册后自动加入默认 `Ruru 社区`
- 社区动态流、帖子详情、发布帖子
- 评论列表、发表评论、删除自己的评论
- 帖子点赞和取消点赞
- 社区成员列表、成员资料页
- 管理员隐藏/恢复帖子和评论、禁言/解除禁言成员
- Flyway 管理数据库迁移，旧业务表通过 V8 自动删除

## 项目结构

```text
study-flow/
  backend/              Spring Boot 后端
  frontend/             React 前端
  docs/                 中文文档
  docker-compose.yml    Docker Compose 编排文件
  .env.example          环境变量模板
```

## 本地开发

后端测试：

```powershell
cd backend
mvn test
```

前端开发：

```powershell
cd frontend
npm install
npm run dev
```

前端开发代理默认转发到 `http://localhost:18080`。如果本机后端运行在其他端口，可以设置：

```powershell
$env:VITE_API_PROXY_TARGET="http://localhost:8080"
npm run dev
```

## Docker 启动

复制环境变量模板：

```bash
cp .env.example .env
```

修改 `.env` 里的数据库密码、JWT 密钥和前端端口，然后启动：

```bash
docker compose up -d --build
```

查看容器：

```bash
docker compose ps
```

## 文档

- 数据库设计：[docs/database.md](docs/database.md)
- 接口文档：[docs/api.md](docs/api.md)
- 部署说明：[docs/deploy.md](docs/deploy.md)
