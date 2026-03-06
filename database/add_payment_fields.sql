-- 添加支付凭证和支付时间字段
ALTER TABLE `order`
ADD COLUMN `payment_proof` VARCHAR(500) COMMENT '支付凭证（转账截图URL）' AFTER `remark`,
ADD COLUMN `payment_time` DATETIME COMMENT '支付时间' AFTER `payment_proof`;
