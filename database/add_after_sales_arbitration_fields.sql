-- Add seller linkage + arbitration fields to after_sales_order
-- Safe to run multiple times: uses INFORMATION_SCHEMA checks.

SET @dbname = DATABASE();
SET @tablename = 'after_sales_order';

SET @col := 'seller_id';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` BIGINT NULL COMMENT ''卖家ID'' AFTER `user_id`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'responsibility';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(20) NULL COMMENT ''责任归因：BUYER/SELLER/LOGISTICS/PLATFORM/UNKNOWN'' AFTER `status`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'arbitration_result';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(500) NULL COMMENT ''仲裁结论'' AFTER `audit_remark`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

