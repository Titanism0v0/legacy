# MySQL 重建与编码修复（schema.sql / data.sql）

本文档覆盖两种方式：
1. 容器首次启动自动初始化（`/docker-entrypoint-initdb.d/`）
2. 随时手动执行“一键重建并校验”脚本（推荐排障）

## 一、首次启动自动初始化（仅首次生效）

`deploy/docker-compose.yml` 会把 `${DEPLOY_DIR}/mysql/init` 挂载到容器 `/docker-entrypoint-initdb.d/`，MySQL 容器首次创建时会执行该目录下 `.sql` 脚本。

### 操作步骤
1. 在服务器创建目录：
   - `/root/overseas-purchase/deploy/mysql/init/`
2. 上传以下文件到该目录：
   - `src/main/resources/db/schema.sql`
   - `src/main/resources/db/data.sql`

## 二、手动执行一键脚本（推荐）

项目已提供脚本：`scripts/mysql_rebuild_and_verify.sh`  
它会自动识别数据库入口：
- 若检测到容器 `overseas-mysql`，使用 `docker exec ... mysql --default-character-set=utf8mb4`
- 否则回退到宿主机 `mysql --default-character-set=utf8mb4`

### 1) 重建模式（清空并重灌，恢复测试商品）
```bash
bash scripts/mysql_rebuild_and_verify.sh --mode rebuild
```

### 2) 仅修复编码模式（不重建表数据）
```bash
bash scripts/mysql_rebuild_and_verify.sh --mode encoding-only
```

### 常用环境变量
- `DB_NAME`（默认 `overseas_purchase`）
- `MYSQL_CONTAINER`（默认 `overseas-mysql`）
- `MYSQL_DOCKER_PASSWORD` / `MYSQL_ROOT_PASSWORD`（容器模式密码）
- `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_USER` / `MYSQL_PASSWORD`（裸机模式）
- `BACKEND_CONTAINER`（默认 `overseas-backend`）
- `BACKEND_LOG_PATH`（裸机日志路径）

示例：
```bash
MYSQL_DOCKER_PASSWORD='你的root密码' \
bash scripts/mysql_rebuild_and_verify.sh --mode rebuild
```

## 三、校验命令（脚本已自动执行）

脚本会自动输出：
- `SHOW VARIABLES LIKE 'character_set_%';`
- `SHOW VARIABLES LIKE 'collation_%';`
- `SELECT id,name FROM category ORDER BY id LIMIT 20;`
- `SELECT COUNT(*) FROM product;`
- 后端最近 200 行日志（Docker 或裸机日志文件）

## 四、注意事项

- `rebuild` 会清空现有业务数据，仅用于测试环境或确认可重建场景。
- 若是线上生产数据，先备份再执行重建。
- 若分类仍乱码，优先检查你导入 SQL 的文件编码是否为 UTF-8（无 BOM 或 UTF-8 BOM 均可）。

