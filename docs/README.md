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

## 后续优化建议

1. 添加Redis缓存提升性能
2. 实现文件上传功能
3. 添加支付功能集成
4. 实现消息通知功能
5. 添加数据统计和报表功能
6. 优化前端页面交互体验
7. 添加单元测试和集成测试

## 许可证

本项目仅用于毕业设计学习使用。
