# StudyFlow 学习任务管理系统

StudyFlow 是一个 Java 全栈学习项目，用来管理个人学习项目、任务、标签和进度统计。

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL、Redis、JWT、Knife4j、JUnit
- 前端：React、TypeScript、Vite、Ant Design、React Router、TanStack Query、Axios
- 部署：Docker、Docker Compose、Nginx、Linux 云服务器

## 核心功能

- 用户注册和登录
- JWT 登录鉴权
- 项目管理
- 任务管理
- 标签管理
- 任务筛选
- 学习进度统计
- Knife4j 接口文档
- Docker Compose 部署

## 项目结构

```text
study-flow/
  backend/       Spring Boot 后端
  frontend/      React 前端
  docs/          中文文档
  docker-compose.yml
  .env.example
```

## 本地开发

后端：

```powershell
cd backend
mvn test
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

前端开发代理默认转发到 `http://localhost:18080`。如果你本机后端跑在其他端口，可以设置：

```powershell
$env:VITE_API_PROXY_TARGET="http://localhost:8080"
npm run dev
```

## Docker 启动

先复制环境变量模板：

```bash
cp .env.example .env
```

修改 `.env` 里的数据库密码和 JWT 密钥，然后启动：

```bash
docker compose up -d --build
```

本机或服务器访问：

```text
http://localhost
```

如果部署到你的服务器，并且域名 `www.surf-violet.com` 已经解析到服务器：

```text
http://www.surf-violet.com
```

## 文档

- 数据库设计：[docs/database.md](docs/database.md)
- 接口文档：[docs/api.md](docs/api.md)
- 部署说明：[docs/deploy.md](docs/deploy.md)
