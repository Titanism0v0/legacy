# Git Push 自动发布（Self-hosted Runner）

本方案用于替换「GitHub 云 Runner 通过 SSH/SCP 推送」模式。  
目标：每次 `push` 到 `main` 后，由云服务器本机 Runner 构建并发布。

## 1. 工作流说明

- 工作流文件：`.github/workflows/auto-deploy.yml`
- 触发方式：`push` 到 `main`（支持手动 `workflow_dispatch`）
- 运行环境：`runs-on: [self-hosted, linux, overseas-prod]`
- 发布行为：
  - 构建前端 `dist` 和后端 `jar`
  - 覆盖 `${DEPLOY_DIR}/frontend/dist` 与 `${DEPLOY_DIR}/backend/app.jar`
  - 重启 `backend` 容器（`docker compose up -d --no-deps backend`）
  - 发布后执行健康检查（`/` 与 `/api/product/list?...`）
  - 健康检查失败自动回滚 `app.jar` 并重启 backend

## 2. 一次性安装 Runner（在云服务器执行）

以下示例目录可按需调整：

```bash
mkdir -p /opt/actions-runner && cd /opt/actions-runner
```

在 GitHub 仓库页面打开：

- `Settings` -> `Actions` -> `Runners` -> `New self-hosted runner`
- 选择 `Linux`，复制页面提供的下载、配置、启动命令到服务器执行

注册时建议使用标签：

- `self-hosted`
- `linux`
- `overseas-prod`

## 3. Runner 权限要求

Runner 运行用户必须具备：

- 可读写部署目录：`/root/overseas-purchase/deploy`（或你的实际 `DEPLOY_DIR`）
- 可执行 `docker compose`（root 或加入 `docker` 组）

若 runner 非 root，至少需要：

```bash
sudo usermod -aG docker <runner_user>
```

并确认部署目录权限允许该用户写入。

## 4. 配置约束

- `deploy/.env` 仅保留在服务器本地，不提交仓库，不由流水线覆盖
- 自动部署不做数据库全量重建
- 数据库结构变更走幂等迁移（应用启动补字段/补表或独立 migration SQL）

## 5. 验证流程

1. 提交一个小变更并 `push origin main`
2. 在 GitHub Actions 查看 `Auto Deploy (Self-hosted Runner)` 日志
3. 云端验证：

```bash
docker ps --filter name=overseas-backend
curl -s "http://127.0.0.1/api/product/list?status=ON_SALE&page=1&size=1"
```

预期包含 `"code":200`。

## 6. 回滚机制

每次发布会先备份旧包到：

- `${DEPLOY_DIR}/backend/backups/app_YYYYmmdd_HHMMSS.jar`

失败时自动恢复该备份并重启 backend。默认保留最近 5 份备份。
