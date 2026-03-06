-- 初始化商品分类（如果数据库中没有分类数据，执行此SQL）

-- 先检查是否已有分类数据
-- SELECT COUNT(*) FROM `category` WHERE `deleted` = 0;

-- 如果分类表为空或没有未删除的分类，执行以下插入语句
INSERT INTO `category` (`name`, `description`, `sort_order`, `deleted`) VALUES
('电子产品', '手机、电脑、数码产品等', 1, 0),
('美妆护肤', '化妆品、护肤品等', 2, 0),
('服装鞋帽', '服装、鞋子、配饰等', 3, 0),
('食品保健', '食品、保健品等', 4, 0),
('母婴用品', '婴儿用品、孕妇用品等', 5, 0),
('其他', '其他商品', 6, 0)
ON DUPLICATE KEY UPDATE `deleted` = 0;

-- 或者如果已有数据但被逻辑删除了，可以恢复：
-- UPDATE `category` SET `deleted` = 0 WHERE `name` IN ('电子产品', '美妆护肤', '服装鞋帽', '食品保健', '母婴用品', '其他');
