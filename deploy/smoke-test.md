# 部署后冒烟测试清单

用于每次本地或服务器部署后快速确认核心链路是否可用。

## 1. 基础服务

- 后端健康：访问 `GET /api/product/list?status=ON_SALE&page=1&size=10`，确认返回成功响应。
- 前端页面：访问首页、登录页、商品详情页，确认静态资源加载正常。
- Redis 配置：当 `PERFORMANCE_REDIS_ENABLED=true` 时，检查后端日志没有 Redis 连接异常。

## 2. 跨境交易链路

- 商品详情：确认能看到跨境计价信息，包括汇率、运费、保险费和税费提示。
- 下单：普通用户提交订单后，订单进入待支付或支付处理中状态。
- 订单洞察：访问订单详情或 `GET /api/order/{id}/insight`，确认费用拆分、税费口径、风控摘要和履约流可返回。

## 3. 履约状态机

- 商家端售出订单：从待发货推进到采购中、已采购、国际运输、清关中、仓库验货、国内配送。
- 物流校验：推进到国际运输必须填写跨境物流号；推进到国内配送或已发货必须填写国内物流号。
- 状态约束：不允许倒退状态，不允许在已完成或已取消订单上继续推进履约。

## 4. 实时提醒

- 买家和商家分别登录不同浏览器窗口。
- 商家推进订单履约状态后，买家侧应收到订单履约更新通知。
- 买家和商家聊天时，未读数能刷新；同一账号打开多个窗口时均可收到 WebSocket 消息。

## 5. 看板与性能

- 管理端工作台：确认待审核、履约中订单、清关/验货待办、订单趋势和状态分布可展示。
- 商家端销售总览：确认订单趋势、热销商品、履约中、清关/验货等指标可展示。
- 性能对比：使用 `scripts/compare_performance.ps1` 对优化前后环境生成 Markdown 报告。

## 6. 自动验证命令

```powershell
mvn -q test
npm.cmd run build
docker compose -f deploy\docker-compose.yml --env-file deploy\.env.example config --quiet
powershell -ExecutionPolicy Bypass -File .\scripts\compare_performance.ps1 -BeforeBaseUrl "http://localhost:8080/api" -Iterations 3 -Warmup 1
```

## 7. 演示数据检查

- 先以只读查询审查 `database/demo_data_cleanup_20260624.sql` 生成的候选记录；不得在生产库直接执行。
- 确认首页不再显示“测试商品”“商家测试”、违禁品标题、空/占位发货地或 Picsum/Unsplash 外链图片。
- 确认至少六个分类有可展示商品，并分别验证分类筛选、关键词搜索、第二页、详情与推荐列表。
- 确认 58 个在售商品均使用不同的 `/demo/products/catalog/*.webp` 本地路径，不再使用六张通用分类 SVG、随机图或素材站运行时外链。
- 对清单中的 58 个图片 URL 逐一请求，确认 HTTP 200 且 `Content-Type` 为 `image/webp`；来源和许可记录见 `frontend/public/demo/products/catalog/SOURCES.md`。
- 检查首页瀑布流保留图片自然比例、卡片不重叠；抽查商品详情、商家商品列表和管理端商品列表缩略图不回退到 `/placeholder.svg`。
- 执行 `node tools/demo-images/verify-catalog.mjs`，确认输出 `PASS: 58 unique local WebP assets verified`。
- 执行图片映射 SQL 前，确认目标数、标题不匹配数和备份数分别为 `58 / 0 / 58`；需要回滚时使用 `database/demo_product_images_20260624_rollback.sql`。

## 8. 违禁内容与 AI 故障降级

- 商品新增/编辑与社区发帖分别测试正常内容、疑似引流内容和明确违规内容。
- 中文和英文分别覆盖毒品、武器、色情、赌博与站外引流；明确违规应拦截或下架，疑似内容应进入人工复核。
- 暂时关闭模型服务或配置不可达地址，确认内容进入人工复核/规则兜底，不能默认发布或上架。
- 分别模拟连接失败、超时与非法 JSON，公开响应只应出现简短提示，不得出现端点、Provider、模型名、堆栈或原始异常。

## 9. 公开接口脱敏与分页一致性

- 匿名访问商品列表、商品详情、推荐与社区列表，确认响应中不含 `auditRemark`、`aiReason`、`moderationProvider`、`moderationModel` 等后台审核细节。
- 商品列表和社区列表均检查 `current`、`size`、`total`、`records`；当 `records` 非空时 `total` 必须大于 0。
- 翻到末页后返回第一页，确认 `records` 数量、`total` 与筛选条件一致。

## 10. favicon 与失败态

- 请求 `GET /favicon.ico`，确认 HTTP 200，`Content-Type` 为 `image/x-icon`、`image/vnd.microsoft.icon` 或等价图标类型，响应体不是 `index.html`。
- 人为使用不存在的商品/社区图片，确认页面自动显示 `/placeholder.svg`。
- 断开后端后检查首页、商品详情和社区页，确认显示空态或简短错误提示，页面不白屏。
