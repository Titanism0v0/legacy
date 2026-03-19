-- Add seller KYC fields into user table
-- Safe to run multiple times: uses INFORMATION_SCHEMA checks.

SET @dbname = DATABASE();
SET @tablename = 'user';

SET @col := 'kyc_status';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(20) NULL COMMENT ''KYC状态：PENDING/APPROVED/REJECTED'' AFTER `role`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'kyc_files';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` TEXT NULL COMMENT ''KYC资料文件URL列表（JSON数组字符串）'' AFTER `kyc_status`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := 'kyc_remark';
SET @stmt := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=@dbname AND table_name=@tablename AND column_name=@col) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @col, '` VARCHAR(255) NULL COMMENT ''KYC审核备注'' AFTER `kyc_files`')
  )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

