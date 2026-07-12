# Ruru 运维手册

这份文档记录 Ruru 上线后最基础的运维动作。目标不是一开始就复杂，而是先保证能发现问题、能备份数据、能恢复服务。

## 健康检查

后端健康接口：

```bash
curl -fsS http://127.0.0.1:8088/api/health
```

Docker Compose 中后端和前端都配置了 `healthcheck`。查看：

```bash
sudo docker compose ps
```

正常应该看到后端、前端、MySQL、Redis、RabbitMQ 都是 `healthy`。

## 日志排查

查看后端日志：

```bash
sudo docker compose logs -f backend
```

查看前端 Nginx 日志：

```bash
sudo docker compose logs -f frontend
```

查看 RabbitMQ 日志：

```bash
sudo docker compose logs -f rabbitmq
```

## MySQL 备份

服务器进入项目目录：

```bash
cd /home/violet/study-flow
```

执行：

```bash
bash scripts/backup-mysql.sh
```

默认备份位置：

```text
backups/mysql/
```

备份文件是 `.sql.gz` 压缩包，不要提交到 GitHub。

## MySQL 恢复

恢复前建议先停掉后端，避免恢复过程中仍有业务写入：

```bash
sudo docker compose stop backend
```

执行恢复：

```bash
bash scripts/restore-mysql.sh backups/mysql/study_flow-YYYYMMDD-HHMMSS.sql.gz
```

恢复后重启：

```bash
sudo docker compose up -d backend
```

## CI 检查

GitHub Actions 会在 push 或 PR 时自动执行：

- 后端测试：`mvn -B test`
- 前端构建：`npm run build`
- 前端 lint：`npm run lint`

如果 CI 失败，不要部署服务器，先修复失败项。

## 当前还需要继续补强

- Prometheus / Grafana 指标监控。
- 请求 traceId，方便把一次请求从 Nginx 追到后端日志。
- 数据库定时备份，可以用 crontab 调用 `scripts/backup-mysql.sh`。
- 恢复演练，确认备份文件真的能还原。
