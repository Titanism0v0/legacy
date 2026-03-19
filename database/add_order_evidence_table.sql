-- Evidence chain for cross-border procurement / inspection / arbitration
-- Safe to run multiple times.

CREATE TABLE IF NOT EXISTS `order_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '证据ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `type` VARCHAR(32) NOT NULL COMMENT '类型：PAYMENT_PROOF/PURCHASE_RECEIPT/WAREHOUSE_PHOTO/TAX_DECLARATION/CHAT_LOG/OTHER',
  `urls` TEXT NULL COMMENT '证据URL列表（JSON数组字符串）',
  `note` VARCHAR(255) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '提交人用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单证据链表';

