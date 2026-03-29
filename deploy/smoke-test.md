# 部署后联调验收（Smoke Test）

假设你的服务已经通过 `docker compose up -d` 启动，并且访问地址为：
- 前端：`http://你的服务器IP/`

## 1) 前端是否正常加载
1. 打开 `http://你的服务器IP/`
2. 确认页面样式、路由切换正常

如果刷新任意路由出现 404：
- 检查 Nginx 是否启用了 `try_files $uri $uri/ /index.html;`

## 2) REST 接口是否走对后端（重点）
1. 打开浏览器开发者工具 -> Network
2. 访问任意需要登录/查询的页面（例如商品列表、订单列表）
3. 确认请求路径前缀是：`/api/...`

常见失败现象：
- 502：Nginx 反代到后端失败（后端容器未启动或未监听 8080）
- 404：Nginx 未代理 `/api/` 或后端 context-path 不一致

## 3) 上传/回显是否正常
1. 打开个人中心（头像上传）
2. 上传 JPG/PNG 并等待“上传成功”
3. 刷新页面/重新登录后，头像仍能回显

如果头像回显 404，通常是：
- backend 容器工作目录/挂载未正确（代码使用 `user.dir + /uploads`）
- `/app/uploads` 没有映射到宿主机的 `backend/uploads`

## 4) WebSocket 聊天是否能连通（重点）
1. 打开聊天页面（例如点击订单里的“联系买家”）
2. 浏览器控制台通常会输出日志（如果有）：
   - `Chat WebSocket connected`
3. 若无法连接，检查：
   - Nginx 是否对 `/api/ws/` 配置了 WebSocket Upgrade：
     - `proxy_http_version 1.1`
     - `proxy_set_header Upgrade $http_upgrade`
     - `proxy_set_header Connection "upgrade"`

后端 WebSocket 最终路径（结合前端代码与后端 context-path）：
- `ws://你的服务器IP/api/ws/chat?token=...`

## 5) 查看后端/NGINX 容器日志
在服务器上执行：
- `docker logs overseas-backend --tail 200`
- `docker logs overseas-nginx --tail 200`

常见排查：
- MySQL 连接失败：检查 `.env` 里的 `DB_URL/DB_USERNAME/DB_PASSWORD` 是否正确，并且 MySQL 服务名是否为 `mysql`
- JWT 失败：检查 `JWT_SECRET`

