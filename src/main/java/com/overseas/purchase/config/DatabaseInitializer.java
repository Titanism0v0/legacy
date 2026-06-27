package com.overseas.purchase.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Checking database schema...");

        ensureChatTables();
        ensureCommunityTables();
        ensureCommunityColumns();
        ensureOrderEvidenceTables();
        ensureAfterSalesAuditLogTable();
        ensureSellerReviewTable();
        ensurePaymentTxnTable();
        ensureExchangeRateTable();

        ensureCategoryColumns();
        ensureProductColumns();
        ensureOrderColumns();
        ensureAfterSalesColumns();
        ensureUserColumns();

        initializeCategoryTaxProfiles();

        log.info("Database schema check completed.");
    }

    private void ensureChatTables() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `chat_session` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`user_a_id` BIGINT NULL," +
                        "`user_b_id` BIGINT NULL," +
                        "`last_message` VARCHAR(500) DEFAULT NULL," +
                        "`last_time` DATETIME DEFAULT NULL," +
                        "`unread_for_a` INT NOT NULL DEFAULT 0," +
                        "`unread_for_b` INT NOT NULL DEFAULT 0," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "UNIQUE KEY `uk_user_pair` (`user_a_id`,`user_b_id`)," +
                        "KEY `idx_user_a_id` (`user_a_id`)," +
                        "KEY `idx_user_b_id` (`user_b_id`)," +
                        "KEY `idx_last_time` (`last_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `chat_message` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`session_id` BIGINT NOT NULL," +
                        "`from_user_id` BIGINT NOT NULL," +
                        "`to_user_id` BIGINT NOT NULL," +
                        "`content` TEXT NOT NULL," +
                        "`content_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT'," +
                        "`send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`read_flag` TINYINT NOT NULL DEFAULT 0," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "KEY `idx_session_id` (`session_id`)," +
                        "KEY `idx_to_user_read` (`to_user_id`,`read_flag`)," +
                        "KEY `idx_send_time` (`send_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );

        ensureColumn("chat_session", "user_a_id",
                "ALTER TABLE `chat_session` ADD COLUMN `user_a_id` BIGINT NULL AFTER `id`");
        ensureColumn("chat_session", "user_b_id",
                "ALTER TABLE `chat_session` ADD COLUMN `user_b_id` BIGINT NULL AFTER `user_a_id`");
        ensureColumn("chat_session", "unread_for_a",
                "ALTER TABLE `chat_session` ADD COLUMN `unread_for_a` INT NOT NULL DEFAULT 0 AFTER `last_time`");
        ensureColumn("chat_session", "unread_for_b",
                "ALTER TABLE `chat_session` ADD COLUMN `unread_for_b` INT NOT NULL DEFAULT 0 AFTER `unread_for_a`");

        migrateLegacyChatSessions();
        ensureIndex("chat_session", "idx_user_a_id", "CREATE INDEX `idx_user_a_id` ON `chat_session` (`user_a_id`)");
        ensureIndex("chat_session", "idx_user_b_id", "CREATE INDEX `idx_user_b_id` ON `chat_session` (`user_b_id`)");
        ensureIndex("chat_session", "uk_user_pair", "CREATE UNIQUE INDEX `uk_user_pair` ON `chat_session` (`user_a_id`, `user_b_id`)");
    }

    private void ensureCommunityTables() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `community_post` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`author_id` BIGINT NOT NULL," +
                        "`author_role` VARCHAR(20) NOT NULL," +
                        "`post_type` VARCHAR(20) NOT NULL," +
                        "`title` VARCHAR(200) NOT NULL," +
                        "`content` TEXT NOT NULL," +
                        "`category_id` BIGINT NOT NULL," +
                        "`content_mode` VARCHAR(20) NOT NULL DEFAULT 'STANDARD'," +
                        "`render_payload` TEXT NULL," +
                        "`images` TEXT NULL," +
                        "`cover_image` VARCHAR(500) NULL," +
                        "`cover_template` VARCHAR(50) NULL," +
                        "`status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'," +
                        "`ai_score` DECIMAL(5,3) NULL," +
                        "`risk_level` VARCHAR(20) NULL," +
                        "`ai_reason` VARCHAR(500) NULL," +
                        "`audit_remark` VARCHAR(500) NULL," +
                        "`moderated_at` DATETIME NULL," +
                        "`moderation_provider` VARCHAR(64) NULL," +
                        "`moderation_model` VARCHAR(64) NULL," +
                        "`comment_count` INT NOT NULL DEFAULT 0," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "KEY `idx_author_id` (`author_id`)," +
                        "KEY `idx_post_type` (`post_type`)," +
                        "KEY `idx_category_id` (`category_id`)," +
                        "KEY `idx_status` (`status`)," +
                        "KEY `idx_create_time` (`create_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `community_comment` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`post_id` BIGINT NOT NULL," +
                        "`author_id` BIGINT NOT NULL," +
                        "`parent_id` BIGINT NULL DEFAULT 0," +
                        "`reply_to_user_id` BIGINT NULL," +
                        "`content` VARCHAR(1000) NOT NULL," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "KEY `idx_post_id` (`post_id`)," +
                        "KEY `idx_parent_id` (`parent_id`)," +
                        "KEY `idx_author_id` (`author_id`)," +
                        "KEY `idx_create_time` (`create_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    private void ensureCommunityColumns() {
        ensureColumn("community_post", "content_mode",
                "ALTER TABLE `community_post` ADD COLUMN `content_mode` VARCHAR(20) NOT NULL DEFAULT 'STANDARD' AFTER `category_id`");
        ensureColumn("community_post", "render_payload",
                "ALTER TABLE `community_post` ADD COLUMN `render_payload` TEXT NULL AFTER `content_mode`");
        ensureColumn("community_post", "ai_score",
                "ALTER TABLE `community_post` ADD COLUMN `ai_score` DECIMAL(5,3) NULL AFTER `status`");
        ensureColumn("community_post", "risk_level",
                "ALTER TABLE `community_post` ADD COLUMN `risk_level` VARCHAR(20) NULL AFTER `ai_score`");
        ensureColumn("community_post", "ai_reason",
                "ALTER TABLE `community_post` ADD COLUMN `ai_reason` VARCHAR(500) NULL AFTER `risk_level`");
        ensureColumn("community_post", "audit_remark",
                "ALTER TABLE `community_post` ADD COLUMN `audit_remark` VARCHAR(500) NULL AFTER `ai_reason`");
        ensureColumn("community_post", "moderated_at",
                "ALTER TABLE `community_post` ADD COLUMN `moderated_at` DATETIME NULL AFTER `audit_remark`");
        ensureColumn("community_post", "moderation_provider",
                "ALTER TABLE `community_post` ADD COLUMN `moderation_provider` VARCHAR(64) NULL AFTER `moderated_at`");
        ensureColumn("community_post", "moderation_model",
                "ALTER TABLE `community_post` ADD COLUMN `moderation_model` VARCHAR(64) NULL AFTER `moderation_provider`");
        ensureIndex("community_post", "idx_status", "CREATE INDEX `idx_status` ON `community_post` (`status`)");
        normalizeLegacyCommunityPostStatus();
    }

    private void normalizeLegacyCommunityPostStatus() {
        try {
            jdbcTemplate.execute(
                    "UPDATE `community_post` " +
                            "SET `content_mode` = 'STANDARD' " +
                            "WHERE `content_mode` IS NULL OR `content_mode` = ''"
            );
            jdbcTemplate.execute(
                    "UPDATE `community_post` " +
                            "SET `status` = 'PUBLISHED' " +
                            "WHERE `deleted` = 0 AND (`status` IS NULL OR `status` = '' OR `status` = 'PENDING_REVIEW')"
            );
        } catch (Exception e) {
            log.warn("Failed to normalize community post status: {}", e.getMessage());
        }
    }

    private void ensureOrderEvidenceTables() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `order_evidence` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`order_id` BIGINT NOT NULL," +
                        "`type` VARCHAR(20) NOT NULL," +
                        "`urls` TEXT NULL," +
                        "`note` TEXT NULL," +
                        "`created_by` BIGINT NULL," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "KEY `idx_order_id` (`order_id`)," +
                        "KEY `idx_created_by` (`created_by`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    private void ensureAfterSalesAuditLogTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `after_sales_audit_log` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`after_sales_id` BIGINT NOT NULL," +
                        "`order_id` BIGINT NOT NULL," +
                        "`operator_role` VARCHAR(20) NOT NULL," +
                        "`operator_id` BIGINT NULL," +
                        "`action` VARCHAR(50) NOT NULL," +
                        "`detail` VARCHAR(1000) NULL," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (`id`)," +
                        "KEY `idx_after_sales_id` (`after_sales_id`)," +
                        "KEY `idx_order_id` (`order_id`)," +
                        "KEY `idx_create_time` (`create_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    private void ensureSellerReviewTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `seller_review` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`order_id` BIGINT NOT NULL," +
                        "`buyer_id` BIGINT NOT NULL," +
                        "`seller_id` BIGINT NOT NULL," +
                        "`rating` INT NOT NULL DEFAULT 5," +
                        "`content` VARCHAR(500) NULL," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "KEY `idx_seller_id` (`seller_id`)," +
                        "KEY `idx_order_id` (`order_id`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    private void ensurePaymentTxnTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `payment_txn` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`order_id` BIGINT NOT NULL," +
                        "`channel` VARCHAR(32) NOT NULL," +
                        "`out_trade_no` VARCHAR(64) NOT NULL," +
                        "`gateway_trade_no` VARCHAR(64) NULL," +
                        "`amount` DECIMAL(10,2) NOT NULL," +
                        "`currency` VARCHAR(10) NOT NULL DEFAULT 'CNY'," +
                        "`status` VARCHAR(32) NOT NULL," +
                        "`qr_code_url` VARCHAR(2048) NULL," +
                        "`expire_time` DATETIME NULL," +
                        "`notify_raw` TEXT NULL," +
                        "`notify_time` DATETIME NULL," +
                        "`refund_status` VARCHAR(32) NULL," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "UNIQUE KEY `uk_out_trade_no` (`out_trade_no`)," +
                        "KEY `idx_order_id` (`order_id`)," +
                        "KEY `idx_status` (`status`)," +
                        "KEY `idx_notify_time` (`notify_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        ensureColumn("payment_txn", "currency",
                "ALTER TABLE `payment_txn` ADD COLUMN `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY' AFTER `amount`");
    }

    private void ensureExchangeRateTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS `exchange_rate` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`currency` VARCHAR(10) NOT NULL," +
                        "`rate_to_cny` DECIMAL(16,6) NOT NULL," +
                        "`source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL'," +
                        "`quote_date` DATE NULL," +
                        "`fetch_time` DATETIME NULL," +
                        "`status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (`id`)," +
                        "UNIQUE KEY `uk_currency` (`currency`)," +
                        "KEY `idx_quote_date` (`quote_date`)," +
                        "KEY `idx_fetch_time` (`fetch_time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        ensureColumn("exchange_rate", "rate_to_cny",
                "ALTER TABLE `exchange_rate` ADD COLUMN `rate_to_cny` DECIMAL(16,6) NOT NULL AFTER `currency`");
        ensureColumn("exchange_rate", "source",
                "ALTER TABLE `exchange_rate` ADD COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' AFTER `rate_to_cny`");
        ensureColumn("exchange_rate", "quote_date",
                "ALTER TABLE `exchange_rate` ADD COLUMN `quote_date` DATE NULL AFTER `source`");
        ensureColumn("exchange_rate", "fetch_time",
                "ALTER TABLE `exchange_rate` ADD COLUMN `fetch_time` DATETIME NULL AFTER `quote_date`");
        ensureColumn("exchange_rate", "status",
                "ALTER TABLE `exchange_rate` ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' AFTER `fetch_time`");
        ensureIndex("exchange_rate", "uk_currency",
                "CREATE UNIQUE INDEX `uk_currency` ON `exchange_rate` (`currency`)");
        ensureIndex("exchange_rate", "idx_quote_date",
                "CREATE INDEX `idx_quote_date` ON `exchange_rate` (`quote_date`)");
        ensureIndex("exchange_rate", "idx_fetch_time",
                "CREATE INDEX `idx_fetch_time` ON `exchange_rate` (`fetch_time`)");
    }

    private void ensureCategoryColumns() {
        ensureColumn("category", "cbec_enabled",
                "ALTER TABLE `category` ADD COLUMN `cbec_enabled` TINYINT NOT NULL DEFAULT 1 AFTER `sort_order`");
        ensureColumn("category", "import_vat_rate",
                "ALTER TABLE `category` ADD COLUMN `import_vat_rate` DECIMAL(8,4) NOT NULL DEFAULT 0.1300 AFTER `cbec_enabled`");
        ensureColumn("category", "consumption_tax_rate",
                "ALTER TABLE `category` ADD COLUMN `consumption_tax_rate` DECIMAL(8,4) NOT NULL DEFAULT 0.0000 AFTER `import_vat_rate`");
        ensureColumn("category", "general_tariff_rate",
                "ALTER TABLE `category` ADD COLUMN `general_tariff_rate` DECIMAL(8,4) NOT NULL DEFAULT 0.1000 AFTER `consumption_tax_rate`");
        ensureColumn("category", "tax_rule_note",
                "ALTER TABLE `category` ADD COLUMN `tax_rule_note` VARCHAR(500) NULL AFTER `general_tariff_rate`");
    }

    private void ensureProductColumns() {
        ensureColumn("product", "currency",
                "ALTER TABLE `product` ADD COLUMN `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY'");
        ensureColumn("product", "audit_status",
                "ALTER TABLE `product` ADD COLUMN `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER `status`");
        ensureColumn("product", "audit_remark",
                "ALTER TABLE `product` ADD COLUMN `audit_remark` VARCHAR(500) NULL AFTER `audit_status`");
        ensureColumn("product", "risk_level",
                "ALTER TABLE `product` ADD COLUMN `risk_level` VARCHAR(20) NOT NULL DEFAULT 'LOW' AFTER `audit_remark`");
        ensureColumn("product", "restricted_flag",
                "ALTER TABLE `product` ADD COLUMN `restricted_flag` TINYINT NOT NULL DEFAULT 0 AFTER `risk_level`");
    }

    private void ensureOrderColumns() {
        ensureColumn("order", "subtotal_price",
                "ALTER TABLE `order` ADD COLUMN `subtotal_price` DECIMAL(10,2) NULL AFTER `quantity`");
        ensureColumn("order", "tax_estimated_amount",
                "ALTER TABLE `order` ADD COLUMN `tax_estimated_amount` DECIMAL(10,2) NULL AFTER `subtotal_price`");
        ensureColumn("order", "shipping_fee_snapshot",
                "ALTER TABLE `order` ADD COLUMN `shipping_fee_snapshot` DECIMAL(10,2) NULL AFTER `tax_estimated_amount`");
        ensureColumn("order", "tax_rate_snapshot",
                "ALTER TABLE `order` ADD COLUMN `tax_rate_snapshot` DECIMAL(8,4) NULL AFTER `total_price`");
        ensureColumn("order", "exchange_rate_snapshot",
                "ALTER TABLE `order` ADD COLUMN `exchange_rate_snapshot` DECIMAL(10,4) NULL AFTER `tax_rate_snapshot`");
        ensureColumn("order", "tax_included_flag",
                "ALTER TABLE `order` ADD COLUMN `tax_included_flag` TINYINT NOT NULL DEFAULT 0 AFTER `exchange_rate_snapshot`");
        ensureColumn("order", "customs_clearance_status",
                "ALTER TABLE `order` ADD COLUMN `customs_clearance_status` VARCHAR(32) NULL AFTER `status`");

        ensureColumn("order", "crossborder_tracking_number",
                "ALTER TABLE `order` ADD COLUMN `crossborder_tracking_number` VARCHAR(100) NULL AFTER `tracking_number`");
        ensureColumn("order", "domestic_tracking_number",
                "ALTER TABLE `order` ADD COLUMN `domestic_tracking_number` VARCHAR(100) NULL AFTER `crossborder_tracking_number`");
        ensureColumn("order", "tax_declaration_accepted",
                "ALTER TABLE `order` ADD COLUMN `tax_declaration_accepted` TINYINT NOT NULL DEFAULT 0 AFTER `tax_included_flag`");
        ensureColumn("order", "restricted_declaration_accepted",
                "ALTER TABLE `order` ADD COLUMN `restricted_declaration_accepted` TINYINT NOT NULL DEFAULT 0 AFTER `tax_declaration_accepted`");
        ensureColumn("order", "audit_status",
                "ALTER TABLE `order` ADD COLUMN `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER `status`");
        ensureColumn("order", "audit_remark",
                "ALTER TABLE `order` ADD COLUMN `audit_remark` VARCHAR(500) NULL AFTER `audit_status`");
        ensureColumn("order", "audit_time",
                "ALTER TABLE `order` ADD COLUMN `audit_time` DATETIME NULL AFTER `audit_remark`");

        ensureColumn("order", "refund_status",
                "ALTER TABLE `order` ADD COLUMN `refund_status` VARCHAR(32) NULL AFTER `payment_time`");
        ensureColumn("order", "refund_amount",
                "ALTER TABLE `order` ADD COLUMN `refund_amount` DECIMAL(10,2) NULL AFTER `refund_status`");
        ensureColumn("order", "refund_time",
                "ALTER TABLE `order` ADD COLUMN `refund_time` DATETIME NULL AFTER `refund_amount`");
        ensureColumn("order", "payment_status",
                "ALTER TABLE `order` ADD COLUMN `payment_status` VARCHAR(32) NULL AFTER `payment_time`");
        ensureColumn("order", "payment_channel",
                "ALTER TABLE `order` ADD COLUMN `payment_channel` VARCHAR(32) NULL AFTER `payment_status`");
        ensureColumn("order", "payment_currency_snapshot",
                "ALTER TABLE `order` ADD COLUMN `payment_currency_snapshot` VARCHAR(10) NOT NULL DEFAULT 'CNY' AFTER `total_price`");
        ensureColumn("order", "international_shipping_fee_snapshot",
                "ALTER TABLE `order` ADD COLUMN `international_shipping_fee_snapshot` DECIMAL(10,2) NULL AFTER `payment_currency_snapshot`");
        ensureColumn("order", "insurance_fee_snapshot",
                "ALTER TABLE `order` ADD COLUMN `insurance_fee_snapshot` DECIMAL(10,2) NULL AFTER `international_shipping_fee_snapshot`");
        ensureColumn("order", "tariff_amount_snapshot",
                "ALTER TABLE `order` ADD COLUMN `tariff_amount_snapshot` DECIMAL(10,2) NULL AFTER `insurance_fee_snapshot`");
        ensureColumn("order", "vat_amount_snapshot",
                "ALTER TABLE `order` ADD COLUMN `vat_amount_snapshot` DECIMAL(10,2) NULL AFTER `tariff_amount_snapshot`");
        ensureColumn("order", "consumption_tax_amount_snapshot",
                "ALTER TABLE `order` ADD COLUMN `consumption_tax_amount_snapshot` DECIMAL(10,2) NULL AFTER `vat_amount_snapshot`");
        ensureColumn("order", "tax_mode_snapshot",
                "ALTER TABLE `order` ADD COLUMN `tax_mode_snapshot` VARCHAR(32) NULL AFTER `consumption_tax_amount_snapshot`");
        ensureColumn("order", "origin_zone_snapshot",
                "ALTER TABLE `order` ADD COLUMN `origin_zone_snapshot` VARCHAR(32) NULL AFTER `tax_mode_snapshot`");
        ensureColumn("order", "payment_submitted_time",
                "ALTER TABLE `order` ADD COLUMN `payment_submitted_time` DATETIME NULL AFTER `payment_channel`");
        ensureColumn("order", "payment_verified_time",
                "ALTER TABLE `order` ADD COLUMN `payment_verified_time` DATETIME NULL AFTER `payment_submitted_time`");
    }

    private void initializeCategoryTaxProfiles() {
        seedCategoryTaxProfile(1L, 1, "0.1300", "0.0000", "0.1000",
                "电子数码类按跨境零售进口优惠口径估算，限值内关税暂按0，主要计征进口增值税。");
        seedCategoryTaxProfile(2L, 1, "0.1300", "0.1500", "0.0500",
                "美妆个护类按跨境零售进口优惠口径估算，部分商品可能涉及消费税，最终以海关审核税则为准。");
        seedCategoryTaxProfile(3L, 1, "0.1300", "0.0000", "0.1600",
                "服饰鞋帽类按跨境零售进口优惠口径估算，限值内关税暂按0，超限值时按一般贸易估算。");
        seedCategoryTaxProfile(4L, 1, "0.0900", "0.0000", "0.1200",
                "食品保健类按跨境零售进口优惠口径估算，适用较低进口增值税率，最终以监管审核为准。");
        seedCategoryTaxProfile(5L, 1, "0.1300", "0.0000", "0.1000",
                "母婴用品类按跨境零售进口优惠口径估算，限值内关税暂按0，税费为预估值。");
        seedCategoryTaxProfile(6L, 1, "0.1300", "0.0000", "0.1500",
                "其他类商品按跨境零售进口优惠口径估算；若不适用跨境清单或超限值，则按一般贸易估算。");
    }

    private void seedCategoryTaxProfile(Long categoryId,
                                        Integer cbecEnabled,
                                        String importVatRate,
                                        String consumptionTaxRate,
                                        String generalTariffRate,
                                        String taxRuleNote) {
        try {
            jdbcTemplate.update(
                    "UPDATE `category` SET " +
                            "`cbec_enabled` = COALESCE(`cbec_enabled`, ?), " +
                            "`import_vat_rate` = CASE WHEN `import_vat_rate` IS NULL OR `import_vat_rate` = 0 THEN ? ELSE `import_vat_rate` END, " +
                            "`consumption_tax_rate` = CASE WHEN `consumption_tax_rate` IS NULL THEN ? ELSE `consumption_tax_rate` END, " +
                            "`general_tariff_rate` = CASE WHEN `general_tariff_rate` IS NULL OR `general_tariff_rate` = 0 THEN ? ELSE `general_tariff_rate` END, " +
                            "`tax_rule_note` = COALESCE(`tax_rule_note`, ?) " +
                            "WHERE `id` = ?",
                    cbecEnabled, importVatRate, consumptionTaxRate, generalTariffRate, taxRuleNote, categoryId
            );
        } catch (Exception e) {
            log.warn("Failed to seed tax profile for category {}: {}", categoryId, e.getMessage());
        }
    }

    private void ensureAfterSalesColumns() {
        ensureColumn("after_sales_order", "seller_id",
                "ALTER TABLE `after_sales_order` ADD COLUMN `seller_id` BIGINT NULL AFTER `user_id`");
        ensureColumn("after_sales_order", "responsibility",
                "ALTER TABLE `after_sales_order` ADD COLUMN `responsibility` VARCHAR(20) NULL AFTER `status`");
        ensureColumn("after_sales_order", "arbitration_result",
                "ALTER TABLE `after_sales_order` ADD COLUMN `arbitration_result` VARCHAR(500) NULL AFTER `audit_remark`");

        ensureColumn("after_sales_order", "evidence_type",
                "ALTER TABLE `after_sales_order` ADD COLUMN `evidence_type` VARCHAR(20) NULL AFTER `images`");
        ensureColumn("after_sales_order", "evidence_urls",
                "ALTER TABLE `after_sales_order` ADD COLUMN `evidence_urls` TEXT NULL AFTER `evidence_type`");
        ensureColumn("after_sales_order", "evidence_text",
                "ALTER TABLE `after_sales_order` ADD COLUMN `evidence_text` TEXT NULL AFTER `evidence_urls`");
        ensureColumn("after_sales_order", "rule_decision",
                "ALTER TABLE `after_sales_order` ADD COLUMN `rule_decision` VARCHAR(20) NULL AFTER `description`");
        ensureColumn("after_sales_order", "rule_reason",
                "ALTER TABLE `after_sales_order` ADD COLUMN `rule_reason` VARCHAR(500) NULL AFTER `rule_decision`");
        ensureColumn("after_sales_order", "ai_score",
                "ALTER TABLE `after_sales_order` ADD COLUMN `ai_score` DECIMAL(5,3) NULL AFTER `rule_reason`");
        ensureColumn("after_sales_order", "ai_suggestion",
                "ALTER TABLE `after_sales_order` ADD COLUMN `ai_suggestion` VARCHAR(20) NULL AFTER `ai_score`");
        ensureColumn("after_sales_order", "ai_reason",
                "ALTER TABLE `after_sales_order` ADD COLUMN `ai_reason` VARCHAR(500) NULL AFTER `ai_suggestion`");
    }

    private void ensureUserColumns() {
        ensureColumn("user", "kyc_status",
                "ALTER TABLE `user` ADD COLUMN `kyc_status` VARCHAR(20) NULL AFTER `role`");
        ensureColumn("user", "kyc_files",
                "ALTER TABLE `user` ADD COLUMN `kyc_files` TEXT NULL AFTER `kyc_status`");
        ensureColumn("user", "kyc_remark",
                "ALTER TABLE `user` ADD COLUMN `kyc_remark` VARCHAR(500) NULL AFTER `kyc_files`");
        ensureColumn("user", "terms_version",
                "ALTER TABLE `user` ADD COLUMN `terms_version` VARCHAR(20) NULL AFTER `country`");
        ensureColumn("user", "terms_accepted_time",
                "ALTER TABLE `user` ADD COLUMN `terms_accepted_time` DATETIME NULL AFTER `terms_version`");
        ensureColumn("user", "privacy_version",
                "ALTER TABLE `user` ADD COLUMN `privacy_version` VARCHAR(20) NULL AFTER `terms_accepted_time`");
        ensureColumn("user", "privacy_accepted_time",
                "ALTER TABLE `user` ADD COLUMN `privacy_accepted_time` DATETIME NULL AFTER `privacy_version`");
    }

    private void migrateLegacyChatSessions() {
        if (!columnExists("chat_session", "buyer_id") || !columnExists("chat_session", "seller_id")) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "UPDATE `chat_session` SET " +
                            "`user_a_id` = CASE WHEN `buyer_id` <= `seller_id` THEN `buyer_id` ELSE `seller_id` END, " +
                            "`user_b_id` = CASE WHEN `buyer_id` <= `seller_id` THEN `seller_id` ELSE `buyer_id` END, " +
                            "`unread_for_a` = CASE WHEN `buyer_id` <= `seller_id` THEN COALESCE(`unread_for_buyer`, 0) ELSE COALESCE(`unread_for_seller`, 0) END, " +
                            "`unread_for_b` = CASE WHEN `buyer_id` <= `seller_id` THEN COALESCE(`unread_for_seller`, 0) ELSE COALESCE(`unread_for_buyer`, 0) END " +
                            "WHERE `user_a_id` IS NULL OR `user_b_id` IS NULL"
            );
        } catch (Exception e) {
            log.warn("Failed to migrate legacy chat sessions: {}", e.getMessage());
        }
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tableName, columnName
        );

        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.execute(alterSql);
            log.info("Added column '{}' to table '{}'", columnName, tableName);
        } catch (Exception e) {
            log.warn("Failed to add column '{}' to '{}': {}", columnName, tableName, e.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tableName, columnName
        );
        return count != null && count > 0;
    }

    private void ensureIndex(String tableName, String indexName, String createSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, tableName, indexName
        );
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.execute(createSql);
        } catch (Exception e) {
            log.warn("Failed to create index '{}' on '{}': {}", indexName, tableName, e.getMessage());
        }
    }
}
