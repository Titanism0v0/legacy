-- 售后申请表
CREATE TABLE IF NOT EXISTS `after_sales_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后单ID',
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
    `seller_id` BIGINT NULL COMMENT '商家ID',
    `type` VARCHAR(20) NOT NULL COMMENT '售后类型：REFUND_ONLY-仅退款，RETURN_GOODS-退货退款',
    `reason` VARCHAR(200) NOT NULL COMMENT '申请原因',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT '退款金额',
    `description` TEXT COMMENT '问题描述',
    `images` TEXT COMMENT '凭证图片列表（JSON格式）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核，APPROVED-审核通过，REJECTED-审核拒绝，COMPLETED-已完成',
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