USE overseas_purchase;
-- 如果 currency 字段不存在，才添加（使用 IF NOT EXISTS 需要 MySQL 8.0+ 或存储过程，这里简单点，直接 ADD，如果存在会报错但不影响后续）
-- 为了保险，直接使用 ALTER TABLE 语句
ALTER TABLE product ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币单位';
