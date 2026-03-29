# 部署（Docker Compose + Nginx + Vue2 + Spring Boot）

本目录给出一套“初始化一次 + 之后 push 自动更新”的部署方式。

假设你在服务器上创建的部署根目录为：
- `DEPLOY_DIR=/root/overseas-purchase/deploy`

需要你在服务器上（一次性）创建这些目录（权限要让 `root` 可写）：
- `$DEPLOY_DIR/frontend/dist`（由 CI 覆盖）
- `$DEPLOY_DIR/backend/app.jar`（由 CI 覆盖，建议命名固定为 `app.jar`）
- `$DEPLOY_DIR/backend/uploads`（上传/回显使用，持久化）
- `$DEPLOY_DIR/mysql/init`（放 `schema.sql` / `data.sql`，用于首次初始化）
- `$DEPLOY_DIR/nginx/default.conf`（由本仓库文件挂载，通常你把 `deploy/` 整体放到服务器即可）

## 1) 服务器端创建运行时环境变量（只需一次）
在 `$DEPLOY_DIR` 放一个 `.env` 文件（示例见本仓库的 `deploy/.env.example`），例如：
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `PAYMENT_DOMAIN`
- `MYSQL_ROOT_PASSWORD`

> 注意：CI/CD 不会帮你生成 `.env`，密钥请自己填写。

## 2) 首次初始化 MySQL
把以下脚本拷贝到 `$DEPLOY_DIR/mysql/init/`：
- `src/main/resources/db/schema.sql`
- （可选）`src/main/resources/db/data.sql`

然后启动 docker-compose，会由 MySQL 容器自动执行 `docker-entrypoint-initdb.d/` 下脚本完成建库建表。

## 3) 启动服务
在 `$DEPLOY_DIR` 目录下执行（确保 docker 与 compose 都可用）：
- `docker compose up -d`

首次启动完成后，浏览器访问：
- `http://你的服务器IP/`

## 4) 后续自动更新（push -> GitHub Actions -> CI）
每次你 push 到 `main` 分支：
1. CI 在 GitHub 构建前端 `dist/`
2. CI 在 GitHub 构建后端 `jar`
3. CI 通过 SSH/SCP 覆盖：
   - `$DEPLOY_DIR/frontend/dist/*`
   - `$DEPLOY_DIR/backend/app.jar`
4. CI 重启 `backend` 容器使新 jar 生效

页面静态资源通常可直接刷新生效；如果遇到缓存，建议浏览器强制刷新。

## 5) 重要路径约定（与你的代码强相关）
- 后端 WebSocket 最终路径：`/api/ws/chat`
- 前端所有 API：`/api/...`
- 上传回显 URL：后端返回 `/api/upload/...`
- 上传文件落盘依赖：代码里使用 `System.getProperty("user.dir") + /uploads/`
  - 因此 backend 容器必须用挂载目录作为工作目录并挂载 `backend/uploads` 到容器 `/app/uploads`

