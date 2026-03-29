SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- 海外代购系统数据库表结构（全量初始化脚本）
-- 适用于 MySQL 8.x，幂等设计，可多次执行

-- 强制清理旧数据（避免乱码残留）
DROP TABLE IF EXISTS `after_sales_order`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `cart`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `security_question`;

-- 1) 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(100) COMMENT '昵称',
  `avatar` VARCHAR(255) COMMENT '头像URL',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/SELLER/ADMIN',
  `country` VARCHAR(20) DEFAULT 'CNH' COMMENT '用户注册国家/地区（对应货币代码）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- （可选）找回密码相关字段，幂等添加
SET @dbname = DATABASE();
SET @tablename = 'user';
-- use_security_questions
SET @columnname = 'use_security_questions';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `use_security_questions` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否使用密保：0-不使用，1-使用'' AFTER `status`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;
-- question1_id
SET @columnname = 'question1_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `question1_id` BIGINT COMMENT ''密保问题1ID'' AFTER `use_security_questions`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;
-- answer1
SET @columnname = 'answer1';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `answer1` VARCHAR(255) COMMENT ''密保答案1'' AFTER `question1_id`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;
-- question2_id
SET @columnname = 'question2_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `question2_id` BIGINT COMMENT ''密保问题2ID'' AFTER `answer1`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;
-- answer2
SET @columnname = 'answer2';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `answer2` VARCHAR(255) COMMENT ''密保答案2'' AFTER `question2_id`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;
-- question3_id
SET @columnname = 'question3_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `question3_id` BIGINT COMMENT ''密保问题3ID'' AFTER `answer2`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;
-- answer3
SET @columnname = 'answer3';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name=@tablename AND table_schema=@dbname AND column_name=@columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `answer3` VARCHAR(255) COMMENT ''密保答案3'' AFTER `question3_id`'
));
PREPARE alterIfNotExists FROM @preparedStatement; EXECUTE alterIfNotExists; DEALLOCATE PREPARE alterIfNotExists;

-- 2) 商品分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `description` VARCHAR(255) COMMENT '分类描述',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID，0表示顶级分类',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 3) 商品表
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `title` VARCHAR(200) NOT NULL COMMENT '商品标题',
  `description` TEXT COMMENT '商品描述',
  `price` DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币单位',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `image` VARCHAR(500) COMMENT '商品主图URL',
  `images` TEXT COMMENT '商品图片列表（JSON格式）',
  `shipping_address` VARCHAR(255) NOT NULL COMMENT '发货地址',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE/OFF_SALE/OUT_OF_STOCK',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 4) 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '商品数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 5) 收货地址表
CREATE TABLE IF NOT EXISTS `address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) NOT NULL COMMENT '省份',
  `city` VARCHAR(50) NOT NULL COMMENT '城市',
  `district` VARCHAR(50) NOT NULL COMMENT '区县',
  `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 6) 订单表
CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
  `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `address_id` BIGINT NOT NULL COMMENT '收货地址ID',
  `quantity` INT NOT NULL COMMENT '商品数量',
  `total_price` DECIMAL(10, 2) NOT NULL COMMENT '订单总价',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
  `tracking_number` VARCHAR(100) COMMENT '运单号',
  `remark` VARCHAR(500) COMMENT '备注',
  `payment_proof` VARCHAR(500) COMMENT '支付凭证（转账截图URL）',
  `payment_time` DATETIME COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 7) 售后申请表
CREATE TABLE IF NOT EXISTS `after_sales_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后单ID',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
  `seller_id` BIGINT NULL COMMENT '商家ID',
  `type` VARCHAR(20) NOT NULL COMMENT '售后类型：REFUND_ONLY/RETURN_GOODS',
  `reason` VARCHAR(200) NOT NULL COMMENT '申请原因',
  `amount` DECIMAL(10, 2) NOT NULL COMMENT '退款金额',
  `description` TEXT COMMENT '问题描述',
  `images` TEXT COMMENT '凭证图片列表（JSON）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/COMPLETED',
  `responsibility` VARCHAR(20) NULL COMMENT '责任归因：BUYER/SELLER/LOGISTICS/PLATFORM/UNKNOWN',
  `audit_remark` VARCHAR(500) COMMENT '审核备注',
  `arbitration_result` VARCHAR(500) NULL COMMENT '仲裁结论',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后申请表';

-- 8) 密保问题表（预置数据按需插入）
CREATE TABLE IF NOT EXISTS `security_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  `question` VARCHAR(255) NOT NULL COMMENT '问题内容',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密保问题表';

-- 初始化管理员账号（密码：admin123，MD5）
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `avatar`) 
VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 'ADMIN', 1, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png');

-- 初始化商品分类（顶级）
INSERT IGNORE INTO `category` (`id`, `name`, `description`, `sort_order`, `parent_id`) VALUES
(1, '电子产品', '手机、电脑、数码产品等', 1, 0),
(2, '美妆护肤', '化妆品、护肤品等', 2, 0),
(3, '服装鞋帽', '服装、鞋子、配饰等', 3, 0),
(4, '食品保健', '食品、保健品等', 4, 0),
(5, '母婴用品', '婴儿用品、孕妇用品等', 5, 0),
(6, '其他', '其他商品', 6, 0);

