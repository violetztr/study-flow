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
study-flow-rabbitmq   RabbitMQ，后续承载视频转码等异步任务
Cloudflare R2         图片/视频对象存储，不在服务器硬盘保存媒体文件
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
RABBITMQ_DEFAULT_USER=ruru
RABBITMQ_DEFAULT_PASS=change-this-rabbitmq-password
R2_ACCOUNT_ID=your-cloudflare-account-id
R2_ACCESS_KEY_ID=your-r2-access-key-id
R2_SECRET_ACCESS_KEY=your-r2-secret-access-key
R2_BUCKET=ruru-community
R2_UPLOAD_URL_TTL=10m
R2_READ_URL_TTL=1h
R2_MAX_IMAGE_BYTES=10485760
R2_MAX_VIDEO_BYTES=209715200
MEDIA_TRANSCODE_ENABLED=false
MEDIA_TRANSCODE_FFMPEG_PATH=ffmpeg
MEDIA_TRANSCODE_WORK_DIR=/tmp/ruru-transcode
MEDIA_TRANSCODE_TIMEOUT=30m
MEDIA_QUEUE_ENABLED=false
MEDIA_QUEUE_FALLBACK_TO_LOCAL_EVENT=true
```

不要把 `.env` 提交到 GitHub。

说明：

- `MEDIA_TRANSCODE_ENABLED=false`：默认不跑真实 FFmpeg，避免服务器还没准备好时误触发转码。
- `MEDIA_TRANSCODE_ENABLED=true`：开启真实转码，后端 Docker 镜像内已安装 FFmpeg。
- `MEDIA_QUEUE_ENABLED=false`：默认使用本地异步事件触发转码。
- `MEDIA_QUEUE_ENABLED=true`：使用 RabbitMQ 发布和消费转码任务。

## Cloudflare R2 配置

R2 Bucket：

```text
ruru-community
```

Bucket 保持私有，后端使用 S3 兼容签名 URL 上传和读取图片、视频和视频封面。

为了让浏览器可以直传 R2，需要在 R2 Bucket 的 CORS 配置中允许站点来源。示例：

```json
[
  {
    "AllowedOrigins": [
      "https://www.violet-surf.com",
      "http://localhost:5173"
    ],
    "AllowedMethods": ["PUT", "GET"],
    "AllowedHeaders": ["content-type"],
    "ExposeHeaders": ["etag"],
    "MaxAgeSeconds": 3600
  }
]
```

如果浏览器上传时报 CORS 错误，优先检查这里。

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
study-flow-frontend   Up   healthy   0.0.0.0:8088->80/tcp
study-flow-backend    Up   healthy
study-flow-mysql      Up   healthy
study-flow-redis      Up   healthy
study-flow-rabbitmq   Up   healthy
```

查看日志：

```bash
sudo docker compose logs -f backend
sudo docker compose logs -f frontend
sudo docker compose logs -f rabbitmq
```

检查后端健康接口：

```bash
curl -fsS http://127.0.0.1:8088/api/health
```

正常会返回 `status` 为 `UP` 的 JSON。这个接口也被 Docker healthcheck 使用。

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

## GitHub Actions

项目已经加入 `.github/workflows/ci.yml`。每次 push 到 `main` 或创建 PR 时会自动执行：

- 后端：`mvn -B test`
- 前端：`npm ci`
- 前端：`npm run build`
- 前端：`npm run lint`

如果 Actions 失败，不要急着部署服务器。先在本地或服务器修复失败项，再重新推送。

## FFmpeg / HLS 上线验证

第一次开启真实转码建议按小步来：

```bash
nano .env
```

先设置：

```env
MEDIA_TRANSCODE_ENABLED=true
MEDIA_QUEUE_ENABLED=false
```

然后：

```bash
sudo docker compose up -d --build
sudo docker compose logs -f backend
```

用一个较小的视频投稿，管理员审核通过后观察日志。确认 HLS 播放正常后，再考虑开启：

```env
MEDIA_QUEUE_ENABLED=true
```

这样做的原因是：先验证 FFmpeg、R2、HLS 主链路，再验证 RabbitMQ 异步队列，排查问题会简单很多。

## 本次 Ruru 化迁移说明

当前版本新增 `V8__remove_legacy_modules.sql`：

- 删除旧学习、任务、笔记、日常、项目中台、GitHub 仓库、公开作品集相关表。
- 把默认社区从 `violet-circle` 升级为 `ruru-community`。
- 把默认话题改为 `公告`、`闲聊`、`求助`、`分享`。

注意：

- 已经在线上执行过的 Flyway 历史迁移不要修改或删除。
- 如果以后还要调整数据库，继续新增 `V9__xxx.sql`、`V10__xxx.sql` 这种新迁移。

当前媒体和 B 站式社区能力主要由这些迁移支撑：

- `V9__add_media_files.sql`：新增 `media_files` 和 `community_post_media`，记录 R2 媒体对象元信息。
- `V10__add_social_wallet_danmaku.sql`：新增钱包、每日奖励、关注关系和弹幕表。
- `V13__add_video_cover_to_posts.sql`：新增视频封面字段。
- `V14__add_post_content_type.sql`：新增图文、视频、直播预留的内容类型。
- `V15__add_post_favorites.sql`：新增收藏表和收藏计数。
- `V16__add_submission_review_fields.sql`：新增投稿审核状态、审核人、审核时间和驳回原因。
- `V17__add_post_view_history.sql`：新增播放上报和观看历史表。
- `V18__add_video_transcode_state.sql`：新增视频转码状态、HLS 清晰度和分片表。

图片和视频真实文件不进 MySQL，也不占服务器硬盘。

## 上线检查

浏览器检查：

```text
https://www.violet-surf.com/login
https://www.violet-surf.com/circle
https://www.violet-surf.com/circle/posts/new
https://www.violet-surf.com/circle/submissions
https://www.violet-surf.com/circle/history
https://www.violet-surf.com/circle/members
```

功能检查：

- 新用户注册后自动加入 Ruru 社区。
- 登录后默认进入 `/circle`。
- 可以发图文、评论、点赞、取消点赞、收藏、投猪币。
- 可以投稿视频，视频和封面上传到 R2 后进入待审核。
- 管理员通过视频后，视频出现在视频频道和详情页。
- 视频播放超过 10 秒或 20% 后才增加播放量，登录用户可以看到观看历史。
- 视频详情页可以发送、关闭、选择颜色和删除弹幕。
- 成员列表可以看到正常注册用户，`DISABLED` 圈内成员不会展示。
- 普通用户看不到管理菜单。
- `ADMIN` 或 `OWNER` 用户可以访问 `/admin/community`，并执行审核、驳回、删除、禁言、解禁等真实管理操作。

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
