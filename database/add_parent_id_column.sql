-- 为category表添加parent_id字段
-- 如果字段已存在会报错，可以忽略

ALTER TABLE `category` 
ADD COLUMN `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID，0表示顶级分类' AFTER `sort_order`;

-- 添加索引
ALTER TABLE `category` 
ADD INDEX `idx_parent_id` (`parent_id`);

-- 将现有分类的parent_id设置为0（表示都是顶级分类）
UPDATE `category` SET `parent_id` = 0 WHERE `parent_id` IS NULL;
