# Ruru Phase 1 生产化部署设计

## 目标

让 `main` 的每次发布使用一个可追溯的 GHCR 镜像 SHA 标签部署到服务器，并提供健康校验、备份、恢复、回滚和统一的运维说明；服务器不再构建 Maven 或 npm 依赖。

## 范围

- GitHub Actions 构建、测试并推送 `backend` 与 `frontend` 镜像到 GHCR。
- 每次发布同时保留 `latest` 和完整 Git SHA 标签；部署使用本次 Git SHA，不使用漂移的 `latest`。
- 服务器使用 `docker-compose.yml` 与 `docker-compose.prod.yml`，按 `git pull --ff-only`、`docker compose pull`、`docker compose up -d --no-build` 部署。
- 部署完成后等待 Compose 健康状态，并请求前端代理下的 `/api/health`。
- 保留并规范 MySQL 备份、恢复和按 SHA 回滚脚本。
- 将 README、部署说明和运维手册统一为同一套生产命令和排障路径。

## 非目标

- 不修改业务接口、数据库模式或媒体转码行为。
- 不在本阶段引入 Kubernetes、监控平台或自动数据库备份调度。
- 不自动执行数据恢复；恢复命令保持显式且需要操作者传入备份文件。

## 发布流程

```text
push main
  -> 后端测试 + 前端构建/lint
  -> 构建 GHCR backend/frontend:<git-sha> 和 :latest
  -> SSH 到服务器
  -> git pull --ff-only
  -> 以 IMAGE_TAG=<git-sha> 拉取镜像
  -> docker compose up -d --no-build
  -> 等待所有服务 healthy
  -> curl 前端 /api/health
```

Docker Compose 的生产覆盖文件只替换 `backend`、`frontend` 的镜像来源；MySQL、Redis、RabbitMQ 继续使用基础 Compose 的持久化配置与健康检查。后端和前端健康检查分别使用 `/api/health` 和 `/`。

## 失败与恢复

- 构建、测试或镜像推送失败时，CD 不连接服务器。
- 服务端 `pull`、`up`、等待健康状态或 HTTP 健康探测失败时，工作流失败并输出当前 Compose 状态和关键容器日志；不在 CI 内自动回滚，避免错误覆盖人工诊断现场。
- 回滚使用 `scripts/rollback.sh <previous-git-sha>`，拉取该 SHA 的两份镜像并以 `--no-build` 重启。
- `scripts/deploy-fast.sh [git-sha]` 为人工部署入口；不传参数时可用 `IMAGE_TAG` 或 `latest`，生产操作手册明确建议传入 SHA。

## 数据保护

- `scripts/backup-mysql.sh` 使用生产 Compose 配置，在 MySQL 容器内执行一致性 `mysqldump`，将压缩 SQL 写入本机 `backups/`。
- `scripts/restore-mysql.sh <backup.sql.gz>` 检查文件存在后将数据导入指定数据库；文档必须明确其写入性质与先备份要求。
- 脚本应同时支持 Docker 组用户和只能使用 `sudo docker` 的服务器用户。

## 验证

- 为 shell 脚本的参数、Compose 文件选择和关键命令增加静态/行为测试，测试不连接真实服务器。
- 本地运行后端 Maven 测试、前端构建和 lint。
- 使用 `docker compose ... config` 验证带 SHA 标签的生产配置展开正确。
- GitHub Actions 仍以 PR 的 CI 作为代码质量门禁；main 的 CD 在质量检查通过后才构建并推送镜像。

## 成功标准

- `main` 推送能构建 GHCR 镜像，且每个镜像都有对应 Git SHA 标签。
- 服务器部署不执行构建，使用 `pull` 与 `up -d --no-build`。
- 部署只有在五个服务健康且 `/api/health` 可用后才成功。
- 运维人员可按文档完成备份、恢复与基于 SHA 的回滚。
- 项目文档中不再将服务器构建镜像作为默认生产发布步骤。
