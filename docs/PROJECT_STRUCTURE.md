# 项目结构开发指南 (PROJECT_STRUCTURE.md)

本文档旨在为开发者提供一份详尽的导航指南，帮助您在新增功能或修复 Bug 时快速定位到相关文件。

---

## 📂 1. 后端工程结构 (Spring Boot)
**根目录**: `src/main/java/com/overseas/purchase`

### 核心开发路径速查

| 功能模块 | Controller (接口层) | Service (业务层) | Mapper (数据层) | Entity (实体类) |
| :--- | :--- | :--- | :--- | :--- |
| **用户/认证** | `UserController.java` | `UserService.java` | `UserMapper.java` | `User.java` |
| **商品管理** | `ProductController.java` | `ProductService.java` | `ProductMapper.java` | `Product.java` |
| **订单交易** | `OrderController.java` | `OrderService.java` | `OrderMapper.java` | `Order.java` |
| **购物车** | `CartController.java` | `CartService.java` | `CartMapper.java` | `Cart.java` |
| **收货地址** | `AddressController.java` | `AddressService.java` | `AddressMapper.java` | `Address.java` |
| **支付/二维码** | `OrderController.java` | `PaymentService.java` | - | - |
| **文件上传** | `UploadController.java` | - | - | - |
| **密保问题** | `SecurityQuestionController.java` | `SecurityQuestionService.java` | `SecurityQuestionMapper.java` | `SecurityQuestion.java` |

### 详细目录说明

#### 1.1 `controller/` - 接口入口
> **职责**: 接收 HTTP 请求，参数校验，权限初步判断，调用 Service，返回统一格式结果。
- **修改建议**: 当需要新增 API 接口（如 `/user/new-api`）或修改入参/出参格式时，请修改此类文件。
- **关键文件**:
  - `GlobalExceptionHandler.java`: **全局异常处理**。如果需要新增自定义异常捕获或修改错误返回格式，请修改此处。
  - `UploadController.java`: **图片上传与读取**。涉及头像上传、商品图片上传及本地文件读取逻辑。

#### 1.2 `service/` - 业务逻辑核心
> **职责**: 处理复杂的业务规则、事务控制、数据组装。
- **修改建议**: 当需要修改核心业务流程（如“下单时扣减库存”、“支付成功后更新状态”）时，请修改此类文件。
- **关键文件**:
  - `PaymentService.java`: 包含生成支付二维码、验证支付凭证的逻辑。

#### 1.3 `mapper/` & `resources/mapper/` - 数据库交互
> **职责**: 定义 SQL 语句。
- **Java 接口**: `src/main/java/.../mapper/`。定义方法签名，继承 `BaseMapper` (MyBatis-Plus)。
- **XML 文件**: `src/main/resources/mapper/`。编写复杂 SQL（如多表关联查询）。
- **修改建议**:
  - 简单 CRUD：直接使用 MyBatis-Plus 内置方法，无需修改。
  - 复杂查询：在 XML 中编写 SQL。例如 `OrderMapper.xml` 中的 `selectOrderList` 关联了商品表和用户表。

#### 1.4 `config/` - 全局配置
> **职责**: 系统级配置。
- `WebConfig.java`: 注册拦截器（Interceptor）。
- `WebMvcConfig.java`: 静态资源映射（目前主要用于 classpath 下的资源，文件上传已改用 Controller 读取）。
- `CorsConfig.java`: 跨域配置。

#### 1.5 `interceptor/` - 请求拦截
- `AuthInterceptor.java`: **权限校验核心**。在此处配置哪些接口需要登录（JWT 校验），哪些接口可以放行（如 `/upload/**`）。

---

## 📂 2. 前端工程结构 (Vue + Element UI)
**根目录**: `frontend/src`

### 页面开发路径速查

| 页面/功能 | 路由路径 | Vue 文件位置 | 备注 |
| :--- | :--- | :--- | :--- |
| **首页** | `/home` | `views/Home.vue` | 商品展示、搜索、分类导航 |
| **登录** | `/login` | `views/Login.vue` | |
| **注册** | `/register` | `views/Register.vue` | 包含国家/地区选择 |
| **商品详情** | `/product/:id` | `views/ProductDetail.vue` | 购买入口、评论（如有） |
| **购物车** | `/cart` | `views/Cart.vue` | 数量调整、结算 |
| **我的订单** | `/orders` | `views/Orders.vue` | 包含支付弹窗、确认收货 |
| **支付页** | `/payment` | `views/Payment.vue` | 扫码支付页面 |
| **个人中心** | `/profile` | `views/UserProfile.vue` | 头像上传、资料修改 |
| **收货地址** | `/address` | `views/Address.vue` | |
| **卖家-商品管理** | `/seller/products` | `views/seller/ProductManage.vue` | 发布、编辑、上下架 |
| **卖家-订单管理** | `/seller/orders` | `views/seller/OrderManage.vue` | 发货操作 |
| **管理员-商品** | `/admin/products` | `views/admin/ProductManage.vue` | 违规下架、批量删除 |
| **管理员-订单** | `/admin/orders` | `views/admin/OrderManage.vue` | 查看所有订单、删除 |
| **管理员-用户** | `/admin/users` | `views/admin/UserManage.vue` | 封禁用户 |

### 核心模块说明

#### 2.1 `api/` - 后端接口对接
> **职责**: 统一管理所有 Axios 请求。
- `index.js`: 包含了所有模块的 API 定义。
- **修改建议**: 当后端新增了 Controller 接口，**必须**在此文件中添加对应的函数（如 `batchDeleteProducts`）。

#### 2.2 `router/index.js` - 路由与权限
> **职责**: 定义页面路径、配置路由守卫（权限控制）。
- **修改建议**:
  - 新增页面：在此注册路由。
  - 权限调整：修改 `meta: { role: [...] }` 来控制哪些角色（USER, SELLER, ADMIN）可以访问该页面。

#### 2.3 `store/index.js` - 全局状态管理 (Vuex)
> **职责**: 管理用户信息、Token、全局配置。
- **State**:
  - `user`: 当前登录用户信息。
  - `token`: JWT 令牌。
  - `currency`: 当前选中的货币/地区（如 CNH, USD）。
- **修改建议**: 如果需要添加全局共享的数据（如“未读消息数”），请修改此处。

#### 2.4 `layouts/MainLayout.vue` - 公共布局
> **职责**: 顶部的导航栏、用户菜单、Logo。
- **修改建议**:
  - 修改导航菜单项（如“隐藏管理员的购物车入口”）。
  - 修改右上角用户下拉菜单。
  - 修改顶部的“地区/货币选择器”。

#### 2.5 `utils/` & `mixins/` - 工具类
- `axios.js`: 请求拦截器（自动带 Token、处理 401 过期）。
- `currency.js`: **汇率配置**。在此处添加新货币或更新汇率。
- `mixins/currencyMixin.js`: 提供 `formatPrice` 方法，用于在组件中自动根据当前地区格式化价格。

---

## 🛠 3. 常见开发场景指引

### 场景 A: 新增一个数据库字段（如给用户增加“积分”）
1.  **数据库**: 修改 `User` 表，添加 `points` 字段。
2.  **后端 Entity**: 修改 `User.java`，添加 `private Integer points;`。
3.  **后端 DTO**: 修改 `UserDTO.java`（如果需要返回给前端）。
4.  **前端 View**: 在 `UserProfile.vue` 中展示积分。

### 场景 B: 开发一个新的后台管理页面
1.  **后端**:
    - 在 Controller 中新增查询接口。
    - 在 Service/Mapper 中实现查询逻辑。
2.  **前端 API**: 在 `api/index.js` 中添加请求方法。
3.  **前端 View**: 在 `views/admin/` 下新建 `.vue` 文件。
4.  **前端 Router**: 在 `router/index.js` 中注册路由，并添加 `meta: { role: ['ADMIN'] }`。
5.  **前端 Layout**: 在 `MainLayout.vue` 的导航栏中添加入口。

### 场景 C: 修改价格显示逻辑
1.  **汇率数据**: 更新 `frontend/src/utils/currency.js`。
2.  **显示格式**: 修改 `frontend/src/mixins/currencyMixin.js`。
3.  **组件使用**: 确保相关组件（如 `Home.vue`, `Cart.vue`）使用了 `{{ formatPrice(price) }}` 而不是硬编码的 `¥{{ price }}`。

---

## ⚠️ 注意事项
*   **重启服务**: 修改后端 Java 代码（Controller, Service, Entity 等）后，必须重启 Spring Boot 应用 (`mvn spring-boot:run`)。
*   **编译前端**: 修改 Vue 文件通常会自动热更新，但如果修改了 `vue.config.js`，需要重启前端服务 (`npm run serve`)。
