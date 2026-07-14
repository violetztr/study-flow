# Ruru 持续部署说明

这套持续部署的目标是：服务器不再每次执行 Maven / npm 编译，只拉取 GitHub Actions 已经构建好的 Docker 镜像并重启容器。

## 部署流程

```text
本地开发
  -> git push main
  -> GitHub Actions 跑 CI
  -> GitHub Actions 构建 backend/frontend 镜像
  -> 推送到 GHCR
  -> GitHub Actions SSH 到服务器
  -> 服务器 git pull
  -> docker compose pull
  -> docker compose up -d --no-build
```

## 镜像地址

```text
ghcr.io/violetztr/study-flow/backend:latest
ghcr.io/violetztr/study-flow/frontend:latest
```

每次 push 到 `main` 时，也会额外推送一份带 commit SHA 的镜像，方便以后回滚。

## GitHub Secrets

进入 GitHub 仓库：

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

需要添加这些 Secrets：

| Secret | 示例 | 说明 |
| --- | --- | --- |
| `DEPLOY_HOST` | `45.56.91.109` | 服务器 IP |
| `DEPLOY_USER` | `violet` | SSH 用户 |
| `DEPLOY_PORT` | `22` | SSH 端口，不填也会默认 22 |
| `DEPLOY_SSH_KEY` | 私钥内容 | 用来登录服务器的 SSH 私钥 |
| `DEPLOY_PATH` | `/home/violet/study-flow` | 服务器项目目录 |
| `GHCR_USERNAME` | `violetztr` | GitHub 用户名 |
| `GHCR_TOKEN` | GitHub PAT | 服务器拉取 GHCR 私有镜像用 |

`GHCR_TOKEN` 建议创建 GitHub Personal Access Token，至少需要 `read:packages` 权限。如果后面包设成公开，也可以不登录拉取，但生产环境建议保留登录。

## 服务器第一次准备

服务器项目目录中保留真实 `.env`：

```bash
cd /home/violet/study-flow
test -f .env && echo ".env exists"
```

GitHub Actions 通过 SSH 部署时不能手动输入 sudo 密码。建议让部署用户可以直接执行 Docker：

```bash
sudo usermod -aG docker violet
```

执行后需要退出 SSH，再重新登录一次，让用户组生效。验证：

```bash
docker ps
```

如果这个命令不需要 `sudo` 就能正常执行，自动部署会更顺畅。

第一次切到镜像部署方式，可以手动验证：

```bash
cd /home/violet/study-flow
git pull --ff-only
sudo docker login ghcr.io -u violetztr
sudo env IMAGE_REGISTRY=ghcr.io/violetztr/study-flow IMAGE_TAG=latest \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml pull backend frontend
sudo env IMAGE_REGISTRY=ghcr.io/violetztr/study-flow IMAGE_TAG=latest \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

以后正常部署不需要手动执行这些命令，push 到 `main` 后 GitHub Actions 会自动做。

## 手动快速部署

如果临时不想等 GitHub Actions 自动 SSH，也可以在服务器执行：

```bash
cd /home/violet/study-flow
git pull --ff-only
bash scripts/deploy-fast.sh <git-sha>
```

这个命令不会在服务器编译代码，会自动等待服务健康并通过 `/api/health` 验证。

## 回滚

GitHub Actions 会同时推送 commit SHA 镜像。回滚到某个 commit：

```bash
cd /home/violet/study-flow
bash scripts/rollback.sh <previous-git-sha>
```

回滚脚本会拉取指定 SHA 的镜像，以 `--no-build` 重启服务，等待健康状态后验证。

如果数据库迁移已经执行过，回滚代码前要先确认 Flyway 迁移是否兼容旧版本。
