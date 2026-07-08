# 部署说明

本文档记录 StudyFlow 使用 GitHub、Docker Compose 和 Linux 云服务器部署的流程。

## 一、部署结构

线上访问链路：

```text
用户浏览器
  -> http://www.surf-violet.com
  -> frontend 容器里的 Nginx
  -> React 静态页面
```

接口请求链路：

```text
浏览器请求 /api
  -> frontend 容器里的 Nginx
  -> backend:8080
  -> Spring Boot
  -> MySQL / Redis
```

Docker Compose 会启动 4 个服务：

```text
study-flow-frontend   前端 Nginx
study-flow-backend    后端 Spring Boot
study-flow-mysql      MySQL
study-flow-redis      Redis
```

## 二、服务器准备

服务器：Ubuntu 20.04。

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

如果当前用户没有 Docker 权限，可以先用 `sudo docker ...`，或者把用户加入 `docker` 用户组。

## 三、从 GitHub 拉取项目

在服务器上执行：

```bash
cd /home/violet
git clone https://github.com/<你的 GitHub 用户名>/study-flow.git
cd study-flow
```

如果已经克隆过，以后更新代码：

```bash
cd /home/violet/study-flow
git pull
```

## 四、配置环境变量

复制模板：

```bash
cp .env.example .env
```

编辑 `.env`：

```bash
nano .env
```

重点修改：

```env
MYSQL_ROOT_PASSWORD=换成你自己的强密码
STUDY_FLOW_JWT_SECRET=换成至少32位的随机字符串
FRONTEND_PORT=80
```

注意：`.env` 不要提交到 GitHub，里面会放真实密码。

## 五、启动服务

```bash
docker compose up -d --build
```

查看容器：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

停止服务：

```bash
docker compose down
```

如果要连数据库数据一起删除：

```bash
docker compose down -v
```

## 六、域名访问

你的域名：

```text
http://www.surf-violet.com
```

需要保证 DNS 的 A 记录指向服务器公网 IP：

```text
45.56.91.109
```

如果服务器已有其他程序占用 80 端口，可以把 `.env` 改成：

```env
FRONTEND_PORT=8088
```

然后访问：

```text
http://www.surf-violet.com:8088
```

正式上线建议最终还是让 80/443 由 Nginx 或反向代理统一管理。

## 七、更新版本

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
docker compose up -d --build
```

## 八、常见排查

查看端口占用：

```bash
sudo lsof -i :80
```

查看后端日志：

```bash
docker compose logs -f backend
```

查看前端 Nginx 日志：

```bash
docker compose logs -f frontend
```

重新构建：

```bash
docker compose build --no-cache
docker compose up -d
```
