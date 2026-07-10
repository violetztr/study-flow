# DevFlow Studio 个人全栈研发中台

DevFlow Studio 是一个面向个人成长和作品展示的全栈项目中台。它把原来的 StudyFlow 学习任务管理系统升级成更完整的研发驾驶舱：既能管理学习项目、任务、笔记和日常计划，也能把一个项目整理成可展示的全栈作品，包括技术栈、线上地址、GitHub 数据、数据库设计、接口文档和公开作品集页面。

StudyFlow 现在是 DevFlow Studio 里的“学习成长模块”，不是被删掉，而是成为更大系统的一部分。

## 线上地址

- 在线访问：https://www.violet-surf.com/login
- 公开作品集：https://www.violet-surf.com/portfolio
- 接口文档：https://www.violet-surf.com/doc.html
- GitHub 仓库：https://github.com/violetztr/study-flow

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL、Redis、JWT、Flyway、Knife4j、JUnit
- 前端：React、TypeScript、Vite、Ant Design、React Router、TanStack Query、Axios
- 部署：Docker、Docker Compose、Nginx、Linux 云服务器

## 核心功能

- 用户注册、登录和 JWT 鉴权
- 学习模块：项目、任务、标签、统计
- 笔记模块：Notion 风格的笔记页和块编辑数据模型
- 日常模块：今日计划、日记、习惯、打卡
- 项目中台：维护项目档案、技术栈、线上地址、文档地址、面试亮点
- GitHub 仓库中心：绑定仓库并同步 star、fork、主语言、README 状态
- 公开作品集：把项目发布成无需登录即可访问的公开展示页
- Docker Compose 一键部署

## 项目结构

```text
study-flow/
  backend/              Spring Boot 后端
  frontend/             React 前端
  docs/                 中文项目文档
  docker-compose.yml    Docker Compose 编排文件
  .env.example          环境变量模板
```

## 架构链路

```text
浏览器
  -> Nginx 主机反向代理
  -> frontend 容器 Nginx
  -> React 页面
  -> /api 请求转发到 backend 容器
  -> Spring Boot
  -> MySQL / Redis
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

## Violet Circle 社区模块

Violet Circle 是 DevFlow Studio 的小圈子社区入口。登录后默认进入 `/circle`，用户可以在圈子里发帖、评论、点赞、查看成员，并继续使用学习、笔记、日常和项目中心等个人工具。

第一阶段已实现：

- 圈子动态流：按置顶和最近活跃时间展示帖子。
- 发帖和帖子详情：支持话题、标题、正文、软删除和作者权限校验。
- 评论：支持评论列表、发表评论、删除自己的评论，并维护帖子评论数。
- 点赞：使用通用 `community_reactions` 表实现帖子点赞和取消点赞。
- 话题：内置学习、笔记、日常、项目等基础话题。
- 成员：注册后自动加入默认圈子，支持成员列表和个人资料展示。
- 管理：管理员可以隐藏/恢复帖子和评论，禁言/解禁成员，并记录审核操作。
- 安全：JWT 鉴权、禁用账号旧 Token 失效、禁言用户只读、管理员接口角色校验。
