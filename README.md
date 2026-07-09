# StudyFlow 学习任务管理系统

StudyFlow 是一个 Java 全栈学习驾驶舱，用来管理个人学习项目、任务、笔记、日常计划和进度统计。项目包含前端页面、后端接口、数据库设计、接口文档、基础测试和 Docker 部署流程，目标是作为可展示、可复盘、可继续迭代的全栈作品。

## 线上地址

- 在线访问：https://www.violet-surf.com/login
- 接口文档：https://www.violet-surf.com/doc.html
- GitHub 仓库：https://github.com/violetztr/study-flow

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL、Redis、JWT、Knife4j、JUnit
- 前端：React、TypeScript、Vite、Ant Design、React Router、TanStack Query、Axios
- 部署：Docker、Docker Compose、Nginx、Linux 云服务器

## 核心功能

- 用户注册、登录和 JWT 鉴权
- 三大模块入口：学习模块、笔记模块、日常模块
- 项目创建、编辑、归档和删除
- 任务创建、编辑、删除和筛选
- 标签创建和任务标签绑定
- 学习任务统计概览
- 笔记工作台和今日计划页面骨架
- Knife4j / Swagger 接口文档
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

前端开发代理默认转发到 `http://localhost:18080`。如果本机后端跑在其他端口，可以设置：

```powershell
$env:VITE_API_PROXY_TARGET="http://localhost:8080"
npm run dev
```

## Docker 启动

先复制环境变量模板：

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
