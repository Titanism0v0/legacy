# 基于Spring Boot的海外代购系统

## 项目简介

本项目是一个基于Spring Boot和Vue.js开发的海外代购系统，实现了用户端、卖家端和管理员端的功能。系统采用前后端分离架构，后端使用Spring Boot + MyBatis-Plus，前端使用Vue.js + Element UI。

## 技术栈

### 后端技术
- Spring Boot 2.7.14
- MyBatis-Plus 3.5.3.1
- MySQL 8.0
- Druid 连接池
- JWT 认证
- Lombok

### 前端技术
- Vue.js 2.6.14
- Vue Router 3.5.1
- Vuex 3.6.2
- Element UI 2.15.13
- Axios 0.27.2

## 已完成功能

### 1. 商品选择与数量控制
- 支持商品数量的增加/减少操作（购物车、商品详情页）
- 购物车内商品数量实时调整
- 立即购买时可选择购买数量

### 2. 实时价格计算
- 购物车自动累计显示商品总价
- 商品小计实时计算（单价 × 数量）
- 订单总价自动汇总

### 3. 订单备注功能
- 支付时可添加转账备注（订单号）
- 支持上传支付凭证URL

### 4. 用户信息管理模块
- 用户登录功能（JWT认证）
- 用户注册功能（支持普通用户/商家角色）
- 密保问题设置（注册时可选）
- 密码找回功能（密保问题找回/邮箱手机找回）

### 5. 商品浏览与筛选
- 首页商品列表展示
- 商品分类筛选
- 商品关键词搜索
- 商品详情查看（价格、库存、发货地、卖家信息）

### 6. 购物车预览功能
- 实时展示已选商品
- 显示商品名称、图片、数量、单价、小计
- 支持删除购物车商品
- 一键结算功能

## 项目结构

```
├── src/main/java/com/overseas/purchase/
│   ├── common/          # 通用类（Result、JwtUtil等）
│   ├── config/          # 配置类（跨域、Web配置等）
│   ├── controller/      # 控制器层
│   ├── dto/             # 数据传输对象
│   ├── entity/          # 实体类
│   ├── interceptor/     # 拦截器
│   ├── mapper/          # Mapper接口
│   └── service/         # 服务层
├── src/main/resources/
│   ├── db/              # 数据库脚本
│   ├── mapper/          # MyBatis XML映射文件
│   └── application.yml  # 配置文件
├── frontend/            # 前端项目
│   ├── src/
│   │   ├── api/         # API接口
│   │   ├── assets/      # 静态资源
│   │   ├── layouts/     # 布局组件
│   │   ├── router/      # 路由配置
│   │   ├── store/       # Vuex状态管理
│   │   ├── utils/       # 工具类
│   │   └── views/       # 页面组件
│   └── package.json
└── pom.xml              # Maven配置文件
```

## 数据库设计

系统包含以下主要数据表：
- `user` - 用户表
- `category` - 商品分类表
- `product` - 商品表
- `cart` - 购物车表
- `address` - 收货地址表
- `order` - 订单表

详细表结构请参考 `src/main/resources/db/schema.sql`

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6+
- Node.js 14+
- MySQL 8.0+

### 后端启动步骤

1. 创建数据库
```sql
CREATE DATABASE overseas_purchase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入数据库脚本
```bash
mysql -u root -p overseas_purchase < src/main/resources/db/schema.sql
```

3. 修改数据库配置
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/overseas_purchase?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

4. 启动后端服务
```bash
mvn spring-boot:run  (请在项目根目录 d:\Programs\legacy 下执行)
```

后端服务将在 `http://localhost:8080` 启动

### 前端启动步骤

1. 进入前端目录
```bash
cd frontend
```

2. 安装依赖
```bash
npm install
```

3. 启动开发服务器
```bash
npm run serve
```

前端服务将在 `http://localhost:8081` 启动

## 默认账号

系统初始化时会创建默认管理员账号：
- 用户名：`admin`
- 密码：`admin123`

## API接口文档

### 用户相关
- `POST /api/user/login` - 用户登录
- `POST /api/user/register` - 用户注册
- `GET /api/user/info` - 获取当前用户信息
- `PUT /api/user/update` - 更新用户信息

### 商品相关
- `GET /api/product/list` - 获取商品列表
- `GET /api/product/detail/{id}` - 获取商品详情
- `POST /api/product/add` - 发布商品（卖家）
- `PUT /api/product/update` - 更新商品
- `PUT /api/product/off-shelf/{id}` - 下架商品
- `PUT /api/product/out-of-stock/{id}` - 标记缺货

### 订单相关
- `POST /api/order/create` - 创建订单
- `POST /api/order/pay/{id}` - 支付订单
- `POST /api/order/ship` - 发货（卖家）
- `POST /api/order/confirm/{id}` - 确认收货
- `GET /api/order/list` - 获取订单列表

### 购物车相关
- `POST /api/cart/add` - 添加到购物车
- `GET /api/cart/list` - 获取购物车列表
- `PUT /api/cart/update` - 更新购物车商品数量
- `DELETE /api/cart/{id}` - 删除购物车商品

## 注意事项

1. 密码加密：当前使用MD5加密，生产环境建议使用BCrypt
2. 文件上传：需要配置文件上传路径，默认路径为 `D:/upload/overseas-purchase/`
3. JWT Token：Token有效期为24小时
4. 跨域配置：已配置允许所有来源，生产环境建议限制特定域名

## 开发规范

1. 代码遵循Java编码规范
2. 使用RESTful API设计风格
3. 统一使用Result类封装响应结果
4. 使用MyBatis-Plus进行数据持久化
5. 前后端分离，通过JSON进行数据交互

## 简历级优化计划

目标：参考“苍穹外卖”这类企业级业务项目的完整度，将本项目从普通商城系统升级为“跨境代购交易与运营管理平台”。重点体现业务闭环、工程化能力、风控意识和数据分析能力，而不是只展示增删改查。

### 对标方向

| 对标能力 | 苍穹外卖典型能力 | 本项目升级方向 |
| --- | --- | --- |
| 多端业务 | 管理端、用户端、小程序端 | 用户端、商家端、管理端三端闭环 |
| 商品运营 | 分类、菜品、套餐、上下架 | 分类、商品、跨境禁限售、商家资质审核 |
| 订单闭环 | 下单、支付、接单、派送、催单 | 下单、跨境计价、支付审核、采购、跨境物流、国内配送、售后仲裁 |
| 实时能力 | 来单提醒、客户催单 | WebSocket 聊天、订单状态提醒、审核待办提醒 |
| 数据统计 | 营业额、订单量、销量排行 | 平台交易额、待审核量、风控商品、商家经营看板 |
| 工程化 | Nginx、接口文档、缓存、部署 | Redis 缓存、Docker Compose、GitHub Actions、Nginx 反代、冒烟测试 |
| 性能治理 | 高频接口优化、线上排查 | JVM 参数配置、GC 日志、慢接口定位、压测对比 |

### 核心改造需求

1. 管理端工作台升级：集中展示待审核订单、待审核商品、待处理售后、社区内容审核、商家 KYC 审核和高风险商品数量。
2. 订单状态机完善：将订单从“待付款/待发货”扩展为“待支付、支付审核、代购采购、跨境运输、清关中、国内配送、完成、售后中”的完整链路。
3. 跨境计价能力强化：沉淀汇率换算、国际运费、保险费、关税/增值税/消费税估算、规则版本快照，形成区别于普通商城的核心亮点。
4. 支付交易闭环：保留手动收款码支付，同时抽象支付宝/微信支付 Provider，支持预支付、交易状态查询、异步回调、退款状态记录。
5. 商家端经营看板：展示订单趋势、成交金额、热销商品、待处理订单和售后任务，让商家端具备运营系统特征。
6. 风控与审核系统：增加禁限售商品识别、敏感词审核、AI 审核兼容接口、审核记录留痕和人工复核入口。
7. 实时沟通与提醒：基于 WebSocket 完善买家与商家聊天、未读消息、订单状态提醒和后台待办提醒。
8. 数据统计与报表：补齐平台维度和商家维度的交易趋势、订单状态分布、商品热度排行、售后率统计。
9. Redis 中间件能力：引入 Redis 缓存首页商品、分类树、汇率快照、热门推荐数据和后台统计摘要，设计缓存失效、预热和兜底查询策略。
10. Redis 业务增强：使用 Redis 实现验证码/临时令牌、防重复提交、订单支付倒计时或待办提醒计数，体现中间件在真实业务中的使用。
11. JVM 性能优化：补充 JVM 启动参数、堆内存配置、GC 日志开启方案，并使用 JVisualVM 或 Arthas 定位慢接口、线程阻塞和内存占用问题。
12. 性能压测与对比：对商品列表、跨境计价、订单查询、后台统计等高频接口进行缓存前后压测，使用 `scripts/compare_performance.ps1` 输出优化前后平均耗时、P95、成功率和 Markdown 对比报告。
13. 前端技术栈升级：将前端从 Vue2 全面升级到 Vue3，同步迁移 Vue Router 4、Pinia、Element Plus，并保留管理端和用户端的清晰路由权限。
14. 工程化交付：完善 README、接口说明、部署文档、Docker Compose、GitHub Actions 自动构建部署和部署后 smoke test。
15. 测试覆盖：补充跨境计价、汇率兜底、订单状态流转、支付状态同步、售后裁决规则的单元测试和核心接口测试。

### 实施计划表

| 阶段 | 改造目标 | 主要修改范围 | 验收方式 | 简历体现 |
| --- | --- | --- | --- | --- |
| P0 基线保护 | 确认当前项目可构建、可回归，避免边改边乱 | `README.md`、`docs/`、`scripts/compare_performance.ps1`、构建脚本 | `mvn -DskipTests compile`、`npm run build`、性能脚本生成基线报告 | 有工程化意识，能用数据衡量优化效果 |
| P1 业务定位升级 | 将项目从普通商城定位为跨境交易平台 | README、项目结构说明、接口说明、演示流程、简历亮点文档 | 文档能清楚说明用户端、商家端、管理端和跨境业务闭环 | 项目不显得像 CRUD，有明确业务复杂度 |
| P2 订单履约链路 | 完善跨境订单状态机和履约节点 | `OrderService`、`OrderController`、`Order`、`OrderMapper.xml`、订单相关 Vue 页面 | 下单后能流转支付审核、采购、跨境运输、清关、国内配送、完成、售后 | 能讲清复杂业务流程和状态机设计 |
| P3 跨境计价与风控 | 强化税费、汇率、运费、禁限售和审核留痕 | `CrossBorderPricingService`、`ExchangeRateService`、`ProductService`、审核服务、相关 DTO | 商品详情、购物车、下单页展示计价拆分和风险提示；后端保存规则快照 | 体现复杂规则建模、合规风控和业务抽象能力 |
| P4 Redis 中间件 | 引入缓存、验证码/令牌、防重复提交和热点数据缓存 | `pom.xml`、`application.yml`、缓存配置、商品/分类/汇率/统计服务 | 压测报告显示缓存前后平均耗时和 P95 改善；缓存失效后数据一致 | 简历可写 Redis 缓存、缓存预热、缓存失效和防重复提交 |
| P5 JVM 与性能治理 | 加入 JVM 参数、GC 日志、慢接口定位和压测对比 | 部署脚本、启动参数、性能文档、压测报告 | 生成优化前后报告，记录响应时间、QPS、数据库访问次数和 GC 日志 | 不只会写业务，还懂基础性能排查 |
| P6 运营数据看板 | 做出平台端和商家端统计分析能力 | `AdminDashboardService`、`SellerDashboardService`、Mapper SQL、ECharts 页面 | 管理端/商家端展示交易额、订单趋势、状态分布、商品排行、待办数据 | 类似苍穹外卖的数据统计和运营后台能力 |
| P7 实时通信与提醒 | 强化 WebSocket 聊天和待办提醒 | `ChatService`、`ChatWebSocketHandler`、前端聊天/通知组件、Nginx WebSocket 配置 | 买家商家可实时聊天，后台待办/未读状态可刷新或推送 | 能体现实时系统和 WebSocket 使用 |
| P8 Vue3 升级 | 将前端技术栈升级到 Vue3 生态 | `frontend/package.json`、`main.js`、`router`、`store`、Element Plus、页面组件 | 前端构建通过，核心页面可访问，路由权限和状态管理正常 | 技术栈更贴近当前招聘要求 |
| P9 测试与交付 | 补测试、部署、冒烟和最终简历材料 | `src/test`、部署文档、GitHub Actions、`deploy/smoke-test.md`、简历项目描述 | 单元测试/构建通过，部署文档完整，生成最终简历描述 | 项目具备可交付、可验证、可讲述的完整度 |

### 执行原则

1. 先做可验证的基线，再做 Redis/JVM/SQL 优化，保证每次优化都有对比数据。
2. 先完成后端业务闭环，再迁移前端 Vue3，避免前后端同时大改导致排查困难。
3. 每个阶段都保留验收标准和可写进简历的成果，最终形成“业务复杂度 + 中间件 + 性能优化 + 工程化交付”的组合亮点。

### 简历呈现目标

项目名称建议写为：跨境代购交易与运营管理平台。

简历描述建议突出：基于 Spring Boot + MyBatis-Plus + Vue2（已规划 Vue3 迁移）的跨境交易平台，包含用户端、商家端和管理端，围绕跨境计价、订单履约、支付审核、内容风控、实时沟通和数据看板构建完整业务闭环；通过 Redis 缓存、WebSocket、Docker Compose、Nginx、GitHub Actions 以及 JVM 参数配置和 GC 日志分析提升系统性能、实时性与部署效率。

### 当前实现进展

| 阶段 | 当前状态 | 已落地内容 |
| --- | --- | --- |
| P0-P1 | 已完成 | 完成项目定位、优化计划、简历呈现文档和性能对比脚本 |
| P2 | 已完成 | 新增跨境订单履约状态机，支持采购、跨境运输、清关、验货、国内配送等节点推进 |
| P3 | 已完成 | 新增订单洞察接口，输出费用拆分、税费口径、风控摘要和履约链路 |
| P4-P5 | 已完成 | 引入 Redis 缓存支撑层、分类/汇率缓存、Docker Redis 服务、JVM 参数和 GC 日志配置 |
| P6 | 已完成 | 管理端和商家端看板增加履约中、清关/验货等跨境运营指标 |
| P7 | 已完成 | WebSocket 支持多端连接，订单履约推进后向买家和商家推送实时提醒 |
| P8 | 待迁移 | Vue3、Vue Router 4、Pinia、Element Plus 已作为独立迁移阶段保留 |
| P9 | 进行中 | 已补充订单状态机和 Redis 缓存单元测试，并保持构建、脚本和部署配置可验证 |

### 性能对比脚本

优化前后可以使用以下脚本生成可复现的性能报告：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/compare_performance.ps1 `
  -BeforeBaseUrl "http://localhost:8080/api" `
  -AfterBaseUrl "http://localhost:8082/api" `
  -EndpointsFile "scripts/performance_endpoints.example.json" `
  -Iterations 50 `
  -Warmup 5
```

脚本会在 `reports/performance/` 下生成原始请求明细、接口汇总 CSV、优化前后对比 CSV 和 Markdown 报告，可用于记录 Redis 缓存、SQL 优化、JVM 参数调整前后的效果。

## 许可证

本项目仅用于毕业设计学习使用。
