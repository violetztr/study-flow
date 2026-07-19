# Ruru 部署回滚说明

当部署出现问题时，按以下步骤回滚到上一个稳定版本。

## 回滚前提

- 服务器上保留最近 3 个版本的 Docker 镜像（GHCR 保留策略确保这点）。
- GitHub Actions CD 流水线每次部署会用 `${{ github.sha }}` 作为镜像标签推送。

## 快速回滚（回退最近一次部署）

### 1. 确认当前运行的版本

```bash
cd /home/violet/study-flow
sudo docker compose ps
```

### 2. 查看最近部署记录

```bash
git log --oneline -10
```

### 3. 回退代码到上一个稳定提交

```bash
# 查看最近两次部署的 commit hash
git log --oneline -2

# 假设上一个稳定版本是 abc1234
git reset --hard abc1234
```

### 4. 用旧版本镜像重新部署

```bash
# 拉取指定版本的镜像（用上一个稳定 commit hash 作为标签）
BACKEND_IMAGE=$(grep -m1 'backend' docker-compose.prod.yml 2>/dev/null || echo 'ghcr.io/你的用户名/study-flow/backend')
FRONTEND_IMAGE=$(grep -m1 'frontend' docker-compose.prod.yml 2>/dev/null || echo 'ghcr.io/你的用户名/study-flow/frontend')

# 如果使用 docker-compose.prod.yml 的生产镜像覆盖方式：
sudo IMAGE_REGISTRY=ghcr.io/你的用户名/study-flow IMAGE_TAG=上一个稳定的commit_hash \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build backend frontend
```

### 5. 验证服务恢复

```bash
# 等待健康检查通过
sleep 30
sudo docker compose ps

# 验证健康接口
curl -fsS http://127.0.0.1:80/api/health
```

## 紧急回滚（docker-compose.prod.yml 可用时）

```bash
cd /home/violet/study-flow

# 查看之前生效的镜像标签
sudo docker images 'ghcr.io/*/study-flow/*' --format '{{.Repository}}:{{.Tag}} {{.CreatedAt}}' | sort -k2 -r

# 选择上一个版本，例如 abc1234
sudo IMAGE_REGISTRY=ghcr.io/你的用户名/study-flow IMAGE_TAG=abc1234 \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build
```

## 数据库回滚

如果部署包含了 Flyway 迁移脚本，回滚代码后数据库 schema 可能不匹配。

### 方案 A：回滚 Flyway（推荐）

如果 Flyway 迁移是可逆的（DROP COLUMN、DROP TABLE 除外）：

```bash
# 查看当前 Flyway 状态
sudo docker compose exec backend mvn flyway:info -f /app/pom.xml 2>/dev/null || echo "请通过数据库直连查看 flyway_schema_history 表"

# 如果需要回滚最近一次迁移：
# 1. 先确认当前版本号
sudo docker compose exec mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" study_flow \
  -e "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"

# 2. 手动执行逆向 SQL
# 3. 删除对应版本的 flyway_schema_history 记录
#    DELETE FROM flyway_schema_history WHERE version = '目标版本号';
```

### 方案 B：数据库快照恢复

```bash
# 从备份恢复（假设备份文件在 /home/violet/backups/）
BACKUP_FILE=$(ls -t /home/violet/backups/study_flow_*.sql.gz | head -1)
gunzip -c "$BACKUP_FILE" | sudo docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" study_flow
```

## 回滚验证清单

- [ ] `docker compose ps` 所有服务 healthy
- [ ] `curl http://127.0.0.1:80/api/health` 返回 200
- [ ] 首页能正常打开
- [ ] 登录功能正常
- [ ] 关键功能可用（发帖、播放视频等）
- [ ] 查看 Grafana 仪表板确认指标正常

## 回滚后通知

1. 在 GitHub 上标记出问题的 PR/commit，说明回滚原因。
2. 如果有告警通知渠道（企业微信、钉钉等），发送回滚通知。
3. 尽快修复问题并重新部署。

## 预防措施

1. **始终在 staging 环境先验证**（如果有的话）。
2. **数据库迁移尽量写成可逆的**。
3. **重大变更前手动备份数据库**：
   ```bash
   sudo docker compose exec mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" study_flow | gzip > /home/violet/backups/study_flow_pre_deploy_$(date +%Y%m%d_%H%M%S).sql.gz
   ```
4. **保留最近 3 个版本的 Docker 镜像**，不要立即清理。
