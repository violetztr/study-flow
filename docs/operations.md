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

## 回滚

GitHub Actions 会同时推送 `latest` 和完整 Git SHA 标签的镜像。回滚到之前的版本：

```bash
cd /home/violet/study-flow
bash scripts/rollback.sh <previous-git-sha>
```

回滚脚本会拉取指定 SHA 的镜像，以 `--no-build` 重启服务，等待所有容器健康状态，并验证前端代理的健康接口。

如果数据库迁移已经执行过，回滚代码前要先确认 Flyway 迁移是否兼容旧版本。

## CI 检查

GitHub Actions 会在 push 或 PR 时自动执行：

- 后端测试：`mvn -B test`
- 前端构建：`npm run build`
- 前端 lint：`npm run lint`

如果 CI 失败，不要部署服务器，先修复失败项。

## 监控（Prometheus + Grafana）

Prometheus 和 Grafana 已在 docker-compose 中配置，部署后可通过以下地址访问：

- **Prometheus**：`http://服务器IP:9090` — 后端指标查询
- **Grafana**：`http://服务器IP:3000` — 仪表板可视化（默认账号 `admin`/`admin`）

首次部署后，Grafana 已自动加载 `Ruru Backend Overview` 仪表板，包含：

- HTTP 请求速率和延迟（p50/p95）
- JVM 内存、CPU、线程
- 活跃请求数、错误率（4xx/5xx）
- 数据库连接池状态
- GC 暂停时间

### 外部访问（可选）

如果需要在公网访问监控面板，在 Nginx 中添加反向代理即可（生产环境必须配置 HTTPS + 认证）：

```nginx
location /grafana/ {
    proxy_pass http://127.0.0.1:3000/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## 日志 traceId

每次 HTTP 请求会自动生成一个 `traceId`（通过 `X-Trace-Id` 响应头返回），并出现在每条日志的 `[traceId]` 字段中。

排查问题时，从浏览器 DevTools 复制 `X-Trace-Id`，然后在服务器上过滤日志：

```bash
sudo docker compose logs backend | grep "your-trace-id"
```

日志同时输出到控制台（纯文本）和 `logs/study-flow.json`（JSON 格式，适合接入 Loki/ELK）。

## 数据库定时备份

建议用 crontab 定时调用备份脚本：

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 3 点备份
0 3 * * * cd /home/violet/study-flow && bash scripts/backup-mysql.sh
```

## 恢复演练

定期验证备份文件可以正常还原，参考 [rollback.md](docs/rollback.md) 中的"数据库快照恢复"章节。
