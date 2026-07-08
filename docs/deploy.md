# 部署说明

本文档记录 StudyFlow 使用 GitHub、Docker Compose、Nginx 和 Linux 云服务器部署的流程。

## 一、当前线上信息

- 服务器 IP：`45.56.91.109`
- 线上访问地址：`https://www.violet-surf.com/login`
- 接口文档地址：`https://www.violet-surf.com/doc.html`
- GitHub 仓库：`https://github.com/violetztr/study-flow`

## 二、部署结构

线上访问链路：

```text
用户浏览器
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

Docker Compose 会启动 4 个服务：

```text
study-flow-frontend   前端 Nginx，宿主机端口 8088
study-flow-backend    后端 Spring Boot
study-flow-mysql      MySQL
study-flow-redis      Redis
```

## 三、服务器准备

服务器系统：Ubuntu 20.04。

需要先安装：

- Git
- Docker
- Docker Compose 插件

安装完成后确认：

```bash
git --version
docker --version
docker compose version
```

如果当前用户没有 Docker 权限，可以先使用 `sudo docker ...`。

## 四、从 GitHub 拉取项目

第一次部署：

```bash
cd /home/violet
git clone https://github.com/violetztr/study-flow.git
cd study-flow
```

如果已经克隆过，以后更新代码：

```bash
cd /home/violet/study-flow
git pull
```

## 五、配置环境变量

复制模板：

```bash
cp .env.example .env
```

编辑 `.env`：

```bash
nano .env
```

生产环境当前使用：

```env
MYSQL_DATABASE=study_flow
MYSQL_ROOT_PASSWORD=换成你自己的强密码
STUDY_FLOW_JWT_SECRET=换成至少32位的随机字符串
STUDY_FLOW_JWT_EXPIRATION_MINUTES=1440
FRONTEND_PORT=8088
```

注意：`.env` 不要提交到 GitHub，里面会放真实密码。

## 六、启动服务

```bash
sudo docker compose up -d --build
```

查看容器：

```bash
sudo docker compose ps
```

正常状态应该能看到：

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

## 七、主域名反向代理

当前服务器的 `443` 使用 Nginx stream 做 SNI 分流：

```text
internet.violet-surf.com -> 127.0.0.1:54431
notion.violet-surf.com   -> 127.0.0.1:5444
其他域名                 -> 127.0.0.1:5443
```

StudyFlow 不修改 `internet.violet-surf.com`，避免影响原有代理服务。

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

修改 Nginx 配置前先备份：

```bash
sudo cp /etc/nginx/sites-available/violet /etc/nginx/sites-available/violet.bak.$(date +%F-%H%M%S)
```

修改后先测试：

```bash
sudo nginx -t
```

通过后再重载：

```bash
sudo systemctl reload nginx
```

## 八、更新版本

本地开发完成后：

```bash
git add .
git commit -m "你的提交信息"
git push
```

服务器更新：

```bash
cd /home/violet/study-flow
git pull
sudo docker compose up -d --build
```

## 九、常见排查

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

查看前端 Nginx 日志：

```bash
sudo docker compose logs -f frontend
```

重新构建：

```bash
sudo docker compose build --no-cache
sudo docker compose up -d
```
