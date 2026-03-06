-- 找回密码功能数据库表结构（安全版本，避免重复执行报错）

-- 1. 创建密保问题表（如果不存在）
CREATE TABLE IF NOT EXISTS `security_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '问题ID',
    `question` VARCHAR(255) NOT NULL COMMENT '问题内容',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密保问题表';

-- 2. 检查并添加用户表字段（分别执行，避免字段已存在时报错）
-- 如果字段已存在，会报错但可以忽略，继续执行下一个

-- 添加 use_security_questions 字段
SET @dbname = DATABASE();
SET @tablename = 'user';
SET @columnname = 'use_security_questions';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否使用密保：0-不使用，1-使用'' AFTER `status`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 question1_id 字段
SET @columnname = 'question1_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` BIGINT COMMENT ''密保问题1ID'' AFTER `use_security_questions`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 answer1 字段
SET @columnname = 'answer1';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` VARCHAR(255) COMMENT ''密保答案1'' AFTER `question1_id`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 question2_id 字段
SET @columnname = 'question2_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` BIGINT COMMENT ''密保问题2ID'' AFTER `answer1`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 answer2 字段
SET @columnname = 'answer2';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` VARCHAR(255) COMMENT ''密保答案2'' AFTER `question2_id`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 question3_id 字段
SET @columnname = 'question3_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` BIGINT COMMENT ''密保问题3ID'' AFTER `answer2`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 answer3 字段
SET @columnname = 'answer3';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `', @columnname, '` VARCHAR(255) COMMENT ''密保答案3'' AFTER `question3_id`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. 初始化密保问题（如果表中没有数据才插入）
INSERT INTO `security_question` (`question`, `sort_order`)
SELECT * FROM (
  SELECT '您出生在哪里？' AS question, 1 AS sort_order
  UNION ALL SELECT '您的小学名称是什么？', 2
  UNION ALL SELECT '您最喜欢的颜色是什么？', 3
  UNION ALL SELECT '您母亲的姓名是什么？', 4
  UNION ALL SELECT '您父亲的姓名是什么？', 5
  UNION ALL SELECT '您最喜欢的食物是什么？', 6
  UNION ALL SELECT '您最喜欢的电影是什么？', 7
  UNION ALL SELECT '您最喜欢的书籍是什么？', 8
  UNION ALL SELECT '您最好的朋友的名字是什么？', 9
  UNION ALL SELECT '您最喜欢的运动是什么？', 10
  UNION ALL SELECT '您最喜欢的音乐类型是什么？', 11
  UNION ALL SELECT '您最喜欢的城市是什么？', 12
  UNION ALL SELECT '您最喜欢的动物是什么？', 13
  UNION ALL SELECT '您最喜欢的季节是什么？', 14
  UNION ALL SELECT '您最喜欢的节日是什么？', 15
) AS tmp
WHERE NOT EXISTS (
  SELECT 1 FROM `security_question` WHERE `question` = tmp.question
);
