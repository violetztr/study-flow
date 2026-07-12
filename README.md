# Ruru 社区

Ruru 社区是一个面向朋友小圈子的 B 站式全栈社区项目。它不是简单的帖子列表，而是围绕“图文、视频、投稿审核、弹幕、播放历史、互动和内容治理”做出的社区平台雏形。

这个项目的目标是练习高级全栈能力：从前端体验、后端业务、数据库建模、对象存储、安全鉴权、Docker 部署，到后续可拆微服务的模块边界。

## 线上地址

- 在线访问：https://www.violet-surf.com/circle
- 登录页面：https://www.violet-surf.com/login
- 接口文档：https://www.violet-surf.com/doc.html
- GitHub 仓库：https://github.com/violetztr/study-flow

## 技术栈

- 前端：React、TypeScript、Vite、Ant Design、React Router、TanStack Query、Axios
- 后端：Java 17、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、Flyway、Knife4j、JUnit
- 数据：MySQL 8、Redis
- 异步任务：RabbitMQ，当前用于视频转码任务队列骨架
- 媒体：Cloudflare R2 对象存储，S3 兼容预签名 URL 上传和读取，FFmpeg/HLS 转码播放链路
- 部署：Docker、Docker Compose、Nginx、Linux 云服务器、GitHub Actions

## 当前核心功能

- 账号系统：注册、登录、JWT 鉴权、每日登录发放投币资产。
- 内容流：直播、图文、视频三频道结构，当前直播为预留模块。
- 图文发布：标题、正文、手动话题、图片上传和图文详情。
- 视频投稿：视频文件、视频封面、标题、简介、话题、上传状态。
- 投稿审核：普通用户投稿进入待审核，ruru / 管理员通过或驳回后才公开。
- 媒体上传：图片和视频直传 Cloudflare R2，业务服务器只保存元数据。
- 视频详情：横屏播放器、作者卡、相关推荐、播放量、弹幕数和评论区。
- 播放统计：播放超过 10 秒或超过 20% 才计一次播放量，支持观看历史。
- 弹幕系统：按时间点飘过、弹幕开关、颜色选择、弹幕列表、管理员删除。
- 社交互动：点赞、投猪币、收藏、关注/取关、成员主页。
- 内容治理：管理员删除帖子/评论/弹幕、处理待审稿件、禁言/解禁成员。
- 文档与测试：数据库设计、接口文档、部署说明、后端自动化测试和前端构建检查。

## 项目结构

```text
study-flow/
  backend/              Spring Boot 后端服务
  frontend/             React 前端应用
  docs/                 中文文档：接口、数据库、部署、面试说明
  docker-compose.yml    MySQL、Redis、RabbitMQ、后端、前端容器编排
  .env.example          服务器环境变量模板
```

## 架构说明

```text
浏览器
  -> Nginx / HTTPS
  -> frontend 容器
  -> /api 反向代理到 backend 容器
  -> Spring Boot
  -> MySQL / Redis / Cloudflare R2
```

媒体文件不经过业务服务器中转。后端只负责校验登录态、生成 R2 预签名上传 URL、记录媒体元数据；浏览器拿到签名 URL 后直接上传到 R2。

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

前端构建和检查：

```powershell
cd frontend
npm run build
npm run lint
```

## Docker 部署

复制环境变量模板：

```bash
cp .env.example .env
```

至少需要配置：

```env
MYSQL_DATABASE=study_flow
MYSQL_ROOT_PASSWORD=change-this-mysql-root-password
STUDY_FLOW_JWT_SECRET=change-this-to-a-long-random-secret-at-least-32-characters
STUDY_FLOW_JWT_EXPIRATION_MINUTES=1440
FRONTEND_PORT=8088
R2_ACCOUNT_ID=your-cloudflare-account-id
R2_ACCESS_KEY_ID=your-r2-access-key-id
R2_SECRET_ACCESS_KEY=your-r2-secret-access-key
R2_BUCKET=ruru-community
R2_UPLOAD_URL_TTL=10m
R2_READ_URL_TTL=1h
R2_MAX_IMAGE_BYTES=10485760
R2_MAX_VIDEO_BYTES=209715200
RABBITMQ_DEFAULT_USER=ruru
RABBITMQ_DEFAULT_PASS=change-this-rabbitmq-password
MEDIA_TRANSCODE_ENABLED=false
MEDIA_QUEUE_ENABLED=false
```

启动：

```bash
sudo docker compose up -d --build
sudo docker compose ps
```

不要把真实 `.env` 或 R2 密钥提交到 GitHub。

## 文档

- 接口文档：[docs/api.md](docs/api.md)
- 数据库设计：[docs/database.md](docs/database.md)
- 部署说明：[docs/deploy.md](docs/deploy.md)
- 运维手册：[docs/operations.md](docs/operations.md)
- 面试展示讲法：[docs/interview.md](docs/interview.md)
- 生产级路线图：[docs/ruru-production-roadmap.md](docs/ruru-production-roadmap.md)
- 生产级任务表：[docs/ruru-production-task-board.md](docs/ruru-production-task-board.md)
- Redis 设计说明：[docs/redis.md](docs/redis.md)
- 视频转码说明：[docs/media-transcode.md](docs/media-transcode.md)
- 阶段 1-2 任务清单：[docs/superpowers/plans/2026-07-11-ruru-bilibili-stage-1-2.md](docs/superpowers/plans/2026-07-11-ruru-bilibili-stage-1-2.md)

## 已完成阶段

阶段 1-3 已完成：B 站式基础体验、视频投稿系统和 Redis 高并发基础。

当前正在推进：

- FFmpeg/HLS 转码链路的线上真实视频验证。
- RabbitMQ 异步转码任务、失败重试和任务状态治理。
- 搜索、热门榜单、推荐雏形。
- 通知系统和消息队列。
- 直播模块和 WebSocket 实时弹幕。
- 监控、日志、备份、CI/CD。
