-- 跨境代购闭环：最小数据库扩展（MVP）
-- 注意：本脚本以“尽量不破坏现有数据”为目标；如遇到字段已存在请手动跳过对应 ALTER。

-- 1) 商品：跨境属性、产地、禁限售标记、税率（用于税费预估）
ALTER TABLE product
    ADD COLUMN is_crossborder TINYINT NOT NULL DEFAULT 0 COMMENT '是否跨境代购商品(0否1是)' AFTER currency,
    ADD COLUMN origin_country VARCHAR(32) NULL COMMENT '产地/来源国家地区' AFTER is_crossborder,
    ADD COLUMN prohibited_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否禁限售(0否1是)' AFTER origin_country,
    ADD COLUMN tax_rate DECIMAL(6,4) NULL COMMENT '预估税率(如0.1300)' AFTER prohibited_flag;

-- 2) 订单：税费预估（不对接海关，仅展示+确认）
ALTER TABLE `order`
    ADD COLUMN tax_estimated_amount DECIMAL(10,2) NULL COMMENT '预估税费金额(展示用)' AFTER total_price,
    ADD COLUMN fulfillment_mode VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '履约模式:NORMAL/CROSSBORDER' AFTER tax_estimated_amount,
    ADD COLUMN intl_tracking_number VARCHAR(64) NULL COMMENT '跨境段物流单号' AFTER tracking_number,
    ADD COLUMN domestic_tracking_number VARCHAR(64) NULL COMMENT '国内段物流单号' AFTER intl_tracking_number;

-- 3) 订单证据链：按订单沉淀凭证（采购/物流/验货/沟通等）
CREATE TABLE IF NOT EXISTS order_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    type VARCHAR(32) NOT NULL COMMENT '证据类型: PURCHASE_PROOF/INTL_TRACKING/WAREHOUSE_INSPECTION/DOMESTIC_TRACKING/OTHER',
    url VARCHAR(500) NOT NULL COMMENT '图片/文件URL',
    note VARCHAR(255) NULL COMMENT '备注',
    uploader_id BIGINT NOT NULL COMMENT '上传者用户ID',
    uploader_role VARCHAR(16) NOT NULL COMMENT '上传者角色: USER/SELLER/ADMIN',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_order_id (order_id),
    KEY idx_type (type)
) COMMENT='订单证据链';

