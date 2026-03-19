-- Add cross-border fulfillment + audit + tax declaration fields to `order`
-- Safe to run multiple times: uses INFORMATION_SCHEMA checks.
-- DB: overseas_purchase

SET @dbname = DATABASE();
SET @tablename = '`order`';

-- helper: add column if missing
SET @col := 'crossborder_tracking_number';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` VARCHAR(64) NULL COMMENT ''跨境运单号'' AFTER `tracking_number`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'domestic_tracking_number';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` VARCHAR(64) NULL COMMENT ''国内运单号'' AFTER `crossborder_tracking_number`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'tax_estimated_amount';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` DECIMAL(10,2) NULL COMMENT ''预估税费'' AFTER `total_price`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'tax_declaration_accepted';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` TINYINT NOT NULL DEFAULT 0 COMMENT ''税费声明是否已确认：0-否，1-是'' AFTER `tax_estimated_amount`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'restricted_declaration_accepted';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` TINYINT NOT NULL DEFAULT 0 COMMENT ''禁限售声明是否已确认：0-否，1-是'' AFTER `tax_declaration_accepted`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'audit_status';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` VARCHAR(20) NULL COMMENT ''审核状态：PENDING/APPROVED/REJECTED'' AFTER `restricted_declaration_accepted`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'audit_remark';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` VARCHAR(255) NULL COMMENT ''审核备注'' AFTER `audit_status`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'audit_time';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name='order' AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @col, '` DATETIME NULL COMMENT ''审核时间'' AFTER `audit_remark`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

