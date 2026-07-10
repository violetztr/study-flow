# 部署说明

本文档记录 Ruru 社区在 Linux 云服务器上使用 GitHub、Docker Compose 和 Nginx 部署的流程。

## 当前线上信息

- 服务器 IP：`45.56.91.109`
- 访问地址：https://www.violet-surf.com/login
- 社区首页：https://www.violet-surf.com/circle
- 接口文档：https://www.violet-surf.com/doc.html
- GitHub 仓库：https://github.com/violetztr/study-flow

## 部署结构

线上访问链路：

```text
浏览器
  -> https://www.violet-surf.com
  -> 服务器 Nginx stream 监听 443
  -> 127.0.0.1:5443
  -> Nginx HTTP server
  -> 127.0.0.1:8088
  -> study-flow-frontend 容器
```

接口请求链路：

```text
浏览器请求 /api
  -> 服务器 Nginx
  -> frontend 容器里的 Nginx
  -> backend:8080
  -> Spring Boot
  -> MySQL / Redis
```

Docker Compose 服务：

```text
study-flow-frontend   前端 Nginx，暴露主机端口 8088
study-flow-backend    后端 Spring Boot
study-flow-mysql      MySQL
study-flow-redis      Redis
```

## 服务器准备

服务器系统：Ubuntu 20.04。

需要安装：

- Git
- Docker
- Docker Compose 插件

确认命令：

```bash
git --version
docker --version
docker compose version
```

如果当前用户没有 Docker 权限，可以先使用 `sudo docker ...`。

## 拉取项目

第一次部署：

```bash
cd /home/violet
git clone https://github.com/violetztr/study-flow.git
cd study-flow
```

以后更新：

```bash
cd /home/violet/study-flow
git pull
```

## 环境变量

复制模板：

```bash
cp .env.example .env
```

编辑：

```bash
nano .env
```

示例：

```env
MYSQL_DATABASE=study_flow
MYSQL_ROOT_PASSWORD=change-this-mysql-root-password
STUDY_FLOW_JWT_SECRET=change-this-to-a-long-random-secret-at-least-32-characters
STUDY_FLOW_JWT_EXPIRATION_MINUTES=1440
FRONTEND_PORT=8088
```

不要把 `.env` 提交到 GitHub。

## 启动服务

```bash
sudo docker compose up -d --build
```

查看容器：

```bash
sudo docker compose ps
```

正常状态应该看到：

```text
study-flow-frontend   Up   0.0.0.0:8088->80/tcp
study-flow-backend    Up
study-flow-mysql      Up   healthy
study-flow-redis      Up   healthy
```

查看日志：

```bash
sudo docker compose logs -f backend
sudo docker compose logs -f frontend
```

停止服务：

```bash
sudo docker compose down
```

如果要连数据库数据一起删除：

```bash
sudo docker compose down -v
```

## Nginx 反向代理

当前服务器的 `443` 使用 Nginx stream 做 SNI 分流：

```text
internet.violet-surf.com -> 127.0.0.1:54431
notion.violet-surf.com   -> 127.0.0.1:5444
其他域名                 -> 127.0.0.1:5443
```

不要修改 `internet.violet-surf.com`，避免影响原来的代理服务。

主域名配置文件：

```text
/etc/nginx/sites-available/violet
```

核心配置：

```nginx
server {
    listen 5443 ssl;
    server_name violet-surf.com www.violet-surf.com;

    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    ssl_certificate /etc/letsencrypt/live/www.violet-surf.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/www.violet-surf.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    listen 80;
    server_name violet-surf.com www.violet-surf.com;
    return 301 https://$host$request_uri;
}
```

修改 Nginx 前先备份：

```bash
sudo cp /etc/nginx/sites-available/violet /etc/nginx/sites-available/violet.bak.$(date +%F-%H%M%S)
```

测试配置：

```bash
sudo nginx -t
```

重载：

```bash
sudo systemctl reload nginx
```

## 更新版本

本地开发完成后：

```bash
git add .
git commit -m "your message"
git push
```

服务器更新：

```bash
cd /home/violet/study-flow
git pull
sudo docker compose up -d --build
sudo docker compose ps
```

Flyway 会在后端启动时自动迁移数据库。

## 本次 Ruru 化迁移说明

当前版本新增 `V8__remove_legacy_modules.sql`：

- 删除旧学习、任务、笔记、日常、项目中台、GitHub 仓库、公开作品集相关表。
- 把默认社区从 `violet-circle` 升级为 `ruru-community`。
- 把默认话题改为 `公告`、`闲聊`、`求助`、`分享`。

注意：

- 已经在线上执行过的 Flyway 历史迁移不要修改或删除。
- 如果以后还要调整数据库，继续新增 `V9__xxx.sql`、`V10__xxx.sql` 这种新迁移。

## 上线检查

浏览器检查：

```text
https://www.violet-surf.com/login
https://www.violet-surf.com/circle
https://www.violet-surf.com/circle/posts/new
https://www.violet-surf.com/circle/members
```

功能检查：

- 新用户注册后自动加入 Ruru 社区。
- 登录后默认进入 `/circle`。
- 可以发帖、评论、点赞、取消点赞。
- 成员列表可以看到正常注册用户，`DISABLED` 圈内成员不会展示。
- 普通用户看不到管理菜单。
- `ADMIN` 或 `OWNER` 用户可以访问 `/admin/community`，并执行隐藏、恢复、禁言、解禁等真实管理操作。

## 常见排查

查看容器：

```bash
sudo docker compose ps
```

查看端口占用：

```bash
sudo lsof -i :80
sudo lsof -i :443
sudo lsof -i :8088
```

查看后端日志：

```bash
sudo docker compose logs -f backend
```

重新构建：

```bash
sudo docker compose build --no-cache
sudo docker compose up -d
```
