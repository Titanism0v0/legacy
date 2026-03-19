-- 一级分类（不会重复）
INSERT INTO category (name, parent_id, sort_order)
SELECT * FROM (
                  SELECT '电子产品', 0, 1 UNION ALL
                  SELECT '美妆护肤', 0, 2 UNION ALL
                  SELECT '服装鞋帽', 0, 3 UNION ALL
                  SELECT '食品保健', 0, 4 UNION ALL
                  SELECT '母婴用品', 0, 5 UNION ALL
                  SELECT '家居家装', 0, 6 UNION ALL
                  SELECT '运动户外', 0, 7 UNION ALL
                  SELECT '图书文娱', 0, 8 UNION ALL
                  SELECT '宠物用品', 0, 9 UNION ALL
                  SELECT '汽车用品', 0, 10 UNION ALL
                  SELECT '办公文具', 0, 11 UNION ALL
                  SELECT '其他', 0, 12
              ) AS tmp(name, parent_id, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.name = tmp.name AND c.parent_id = 0
);

-- 电子产品
INSERT INTO category (name, parent_id)
SELECT '手机', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '平板电脑', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '笔记本电脑', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '台式电脑', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '耳机', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '智能手表', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '相机', c.id FROM category c WHERE c.name='电子产品'
UNION ALL SELECT '游戏主机', c.id FROM category c WHERE c.name='电子产品';

-- 美妆护肤
INSERT INTO category (name, parent_id)
SELECT '护肤品', c.id FROM category c WHERE c.name='美妆护肤'
UNION ALL SELECT '彩妆', c.id FROM category c WHERE c.name='美妆护肤'
UNION ALL SELECT '香水', c.id FROM category c WHERE c.name='美妆护肤';

-- 服装鞋帽
INSERT INTO category (name, parent_id)
SELECT '男装', c.id FROM category c WHERE c.name='服装鞋帽'
UNION ALL SELECT '女装', c.id FROM category c WHERE c.name='服装鞋帽'
UNION ALL SELECT '鞋子', c.id FROM category c WHERE c.name='服装鞋帽'
UNION ALL SELECT '箱包', c.id FROM category c WHERE c.name='服装鞋帽';

-- 食品保健
INSERT INTO category (name, parent_id)
SELECT '零食', c.id FROM category c WHERE c.name='食品保健'
UNION ALL SELECT '保健品', c.id FROM category c WHERE c.name='食品保健'
UNION ALL SELECT '饮料', c.id FROM category c WHERE c.name='食品保健';

-- 母婴用品
INSERT INTO category (name, parent_id)
SELECT '奶粉', c.id FROM category c WHERE c.name='母婴用品'
UNION ALL SELECT '尿不湿', c.id FROM category c WHERE c.name='母婴用品'
UNION ALL SELECT '玩具', c.id FROM category c WHERE c.name='母婴用品';

-- 家居家装
INSERT INTO category (name, parent_id)
SELECT '家具', c.id FROM category c WHERE c.name='家居家装'
UNION ALL SELECT '家电', c.id FROM category c WHERE c.name='家居家装'
UNION ALL SELECT '装饰品', c.id FROM category c WHERE c.name='家居家装';

-- 运动户外
INSERT INTO category (name, parent_id)
SELECT '运动鞋', c.id FROM category c WHERE c.name='运动户外'
UNION ALL SELECT '健身器材', c.id FROM category c WHERE c.name='运动户外'
UNION ALL SELECT '户外装备', c.id FROM category c WHERE c.name='运动户外';

-- 图书文娱
INSERT INTO category (name, parent_id)
SELECT '图书', c.id FROM category c WHERE c.name='图书文娱'
UNION ALL SELECT '游戏', c.id FROM category c WHERE c.name='图书文娱'
UNION ALL SELECT '影视', c.id FROM category c WHERE c.name='图书文娱';

-- 宠物用品
INSERT INTO category (name, parent_id)
SELECT '猫粮', c.id FROM category c WHERE c.name='宠物用品'
UNION ALL SELECT '狗粮', c.id FROM category c WHERE c.name='宠物用品'
UNION ALL SELECT '宠物玩具', c.id FROM category c WHERE c.name='宠物用品';

-- 汽车用品
INSERT INTO category (name, parent_id)
SELECT '汽车装饰', c.id FROM category c WHERE c.name='汽车用品'
UNION ALL SELECT '机油', c.id FROM category c WHERE c.name='汽车用品';

-- 办公文具
INSERT INTO category (name, parent_id)
SELECT '笔', c.id FROM category c WHERE c.name='办公文具'
UNION ALL SELECT '本子', c.id FROM category c WHERE c.name='办公文具'
UNION ALL SELECT '打印机', c.id FROM category c WHERE c.name='办公文具';

-- 其他
INSERT INTO category (name, parent_id)
SELECT '其他商品', c.id FROM category c WHERE c.name='其他';


ALTER TABLE category ADD UNIQUE KEY unique_name_parent (name, parent_id);