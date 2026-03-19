-- Add product audit / risk / restricted flags
-- Safe to run multiple times: uses INFORMATION_SCHEMA checks.

SET @dbname = DATABASE();
SET @tablename = 'product';

SET @col := 'audit_status';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(20) NULL COMMENT ''审核状态：PENDING/APPROVED/REJECTED'' AFTER `status`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'audit_remark';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(255) NULL COMMENT ''审核/下架原因'' AFTER `audit_status`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'risk_level';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(10) NULL COMMENT ''风险等级：LOW/MEDIUM/HIGH'' AFTER `audit_remark`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'restricted_flag';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` TINYINT NOT NULL DEFAULT 0 COMMENT ''禁限售标记：0-否，1-是'' AFTER `risk_level`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

