-- 为商品分类表添加父分类字段（如果还没有的话）
-- 注意：MySQL 8.0+ 支持 IF NOT EXISTS，旧版本可能需要手动检查

-- 先检查字段是否存在（如果不存在则添加）
SET @dbname = DATABASE();
SET @tablename = 'category';
SET @columnname = 'parent_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1', -- 字段已存在，不执行任何操作
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT DEFAULT 0 COMMENT ''父分类ID，0表示顶级分类'' AFTER sort_order')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为parent_id添加索引（如果不存在）
SET @indexname = 'idx_parent_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD INDEX ', @indexname, ' (parent_id)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 将现有的大分类的parent_id设置为0（如果为NULL）
UPDATE `category` SET `parent_id` = 0 WHERE `parent_id` IS NULL AND `deleted` = 0;

-- 查询现有的大分类ID并插入细分类
-- 为"电子产品"添加细分类
INSERT INTO `category` (`name`, `description`, `parent_id`, `sort_order`, `deleted`)
SELECT '手机', '智能手机、功能手机等', id, 1, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '电脑', '笔记本电脑、台式机、一体机等', id, 2, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '平板电脑', 'iPad、安卓平板等', id, 3, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '数码相机', '单反相机、微单相机、卡片机等', id, 4, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '耳机音响', '耳机、音箱、音响设备等', id, 5, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '智能穿戴', '智能手表、手环、VR设备等', id, 6, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '电子配件', '充电器、数据线、保护壳等', id, 7, 0 FROM `category` WHERE `name` = '电子产品' AND `deleted` = 0 LIMIT 1;

-- 为"美妆护肤"添加细分类
INSERT INTO `category` (`name`, `description`, `parent_id`, `sort_order`, `deleted`)
SELECT '面部护肤', '洁面、爽肤水、精华、面霜等', id, 1, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '彩妆', '粉底、口红、眼影、腮红等', id, 2, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '香水', '女士香水、男士香水、淡香水等', id, 3, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '身体护理', '沐浴露、身体乳、护手霜等', id, 4, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '美发护发', '洗发水、护发素、发膜等', id, 5, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '美甲', '指甲油、美甲工具等', id, 6, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '美容工具', '化妆刷、美容器具等', id, 7, 0 FROM `category` WHERE `name` = '美妆护肤' AND `deleted` = 0 LIMIT 1;

-- 为"服装鞋帽"添加细分类
INSERT INTO `category` (`name`, `description`, `parent_id`, `sort_order`, `deleted`)
SELECT '女装', '连衣裙、上衣、裤子、外套等', id, 1, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '男装', 'T恤、衬衫、西装、休闲装等', id, 2, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '女鞋', '高跟鞋、平底鞋、运动鞋等', id, 3, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '男鞋', '皮鞋、运动鞋、休闲鞋等', id, 4, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '箱包', '手提包、背包、旅行箱等', id, 5, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '配饰', '帽子、围巾、腰带、眼镜等', id, 6, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '内衣', '文胸、内裤、睡衣等', id, 7, 0 FROM `category` WHERE `name` = '服装鞋帽' AND `deleted` = 0 LIMIT 1;

-- 为"食品保健"添加细分类
INSERT INTO `category` (`name`, `description`, `parent_id`, `sort_order`, `deleted`)
SELECT '进口零食', '巧克力、饼干、糖果等', id, 1, 0 FROM `category` WHERE `name` = '食品保健' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '营养保健品', '维生素、蛋白粉、鱼油等', id, 2, 0 FROM `category` WHERE `name` = '食品保健' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '茶叶咖啡', '进口茶叶、咖啡豆、咖啡粉等', id, 3, 0 FROM `category` WHERE `name` = '食品保健' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '酒类', '红酒、威士忌、清酒等', id, 4, 0 FROM `category` WHERE `name` = '食品保健' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '有机食品', '有机米面、有机蔬菜等', id, 5, 0 FROM `category` WHERE `name` = '食品保健' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '调味品', '进口调料、酱料等', id, 6, 0 FROM `category` WHERE `name` = '食品保健' AND `deleted` = 0 LIMIT 1;

-- 为"母婴用品"添加细分类
INSERT INTO `category` (`name`, `description`, `parent_id`, `sort_order`, `deleted`)
SELECT '婴儿奶粉', '进口奶粉、配方奶粉等', id, 1, 0 FROM `category` WHERE `name` = '母婴用品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '婴儿辅食', '米粉、果泥、肉泥等', id, 2, 0 FROM `category` WHERE `name` = '母婴用品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '婴儿用品', '奶瓶、尿不湿、湿巾等', id, 3, 0 FROM `category` WHERE `name` = '母婴用品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '儿童玩具', '益智玩具、毛绒玩具等', id, 4, 0 FROM `category` WHERE `name` = '母婴用品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '孕妇用品', '孕妇装、防辐射服等', id, 5, 0 FROM `category` WHERE `name` = '母婴用品' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '儿童服装', '童装、童鞋等', id, 6, 0 FROM `category` WHERE `name` = '母婴用品' AND `deleted` = 0 LIMIT 1;

-- 为"其他"添加细分类
INSERT INTO `category` (`name`, `description`, `parent_id`, `sort_order`, `deleted`)
SELECT '家居用品', '收纳、清洁用品等', id, 1, 0 FROM `category` WHERE `name` = '其他' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '运动户外', '运动装备、户外用品等', id, 2, 0 FROM `category` WHERE `name` = '其他' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '图书音像', '进口图书、CD、DVD等', id, 3, 0 FROM `category` WHERE `name` = '其他' AND `deleted` = 0 LIMIT 1
UNION ALL
SELECT '汽车用品', '汽车配件、装饰品等', id, 4, 0 FROM `category` WHERE `name` = '其他' AND `deleted` = 0 LIMIT 1;
