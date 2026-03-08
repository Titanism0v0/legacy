-- ============================================================
-- 生成 1000 条真实商品数据（不会删除原有商品）
-- 发布者：商家测试1
-- ============================================================

SET NAMES utf8mb4;

-- 获取商家ID
SET @seller_id = (
  SELECT id
  FROM `user`
  WHERE nickname = '商家测试1'
  AND deleted = 0
  LIMIT 1
);

SELECT IF(
  @seller_id IS NULL,
  '错误：未找到【商家测试1】',
  CONCAT('使用 seller_id=',@seller_id,' 生成商品')
) AS 提示;


-- 使用递归CTE生成1-1000序列
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n+1 FROM seq WHERE n < 1000
)

INSERT INTO product
(seller_id,category_id,title,description,price,stock,image,shipping_address,status)

SELECT
  @seller_id,

  FLOOR(1 + RAND()*6),

  ELT(FLOOR(1+RAND()*20),

  'Apple iPhone 15 Pro Max 256GB',
  'Sony WH-1000XM5 无线降噪耳机',
  'Apple MacBook Air M2 13英寸',
  'Nintendo Switch OLED 主机',
  'Apple AirPods Pro 第二代',
  'SK-II 神仙水 230ml',
  'La Mer 海蓝之谜面霜',
  'Estee Lauder 小棕瓶精华',
  'CHANEL 蔚蓝男士香水',
  '资生堂 红腰子精华',
  'Nike Air Jordan 1 复刻高帮',
  'Adidas Yeezy Boost 350 V2',
  'Canada Goose 羽绒服',
  'Lululemon Align 瑜伽裤',
  'Burberry 羊绒围巾',
  'Dyson V15 Detect 吸尘器',
  'RIMOWA 铝镁合金行李箱',
  'Kindle Paperwhite 电子书',
  'LEGO 霍格沃茨城堡',
  'MUJI 无印良品香薰机'
  ),

  '海外代购正品商品，支持国际直邮，保证正品。',

  ROUND(100 + RAND()*8000,2),

  FLOOR(10 + RAND()*300),

  ELT(FLOOR(1+RAND()*20),

  'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', -- iphone
  'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800', -- sony headphone
  'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', -- macbook
  'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800', -- switch
  'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800', -- airpods
  'https://images.unsplash.com/photo-1571781926291-c477ebfd024b?w=800',
  'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=800',
  'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=800',
  'https://images.unsplash.com/photo-1541643600914-78b084683601?w=800',
  'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800',
  'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800',
  'https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?w=800',
  'https://images.unsplash.com/photo-1539533018447-63fcce2678e3?w=800',
  'https://images.unsplash.com/photo-1506629082955-511b1aa562c8?w=800',
  'https://images.unsplash.com/photo-1520903920243-00d872a2d1c9?w=800',
  'https://images.unsplash.com/photo-1558317374-067fb5f30001?w=800',
  'https://images.unsplash.com/photo-1565026057447-bc90a3dceb87?w=800',
  'https://images.unsplash.com/photo-1592496001020-d31bd830651f?w=800',
  'https://images.unsplash.com/photo-1587654780291-39c9404d746b?w=800',
  'https://images.unsplash.com/photo-1602928321679-560bb453f190?w=800'
  ),

  ELT(FLOOR(1 + RAND()*8),
  '日本东京',
  '美国洛杉矶',
  '美国纽约',
  '法国巴黎',
  '英国伦敦',
  '德国柏林',
  '加拿大多伦多',
  '澳大利亚悉尼'
  ),

  'ON_SALE'

FROM seq;