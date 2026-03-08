-- ============================================================
-- 商家测试1 的商品数据（仿原 insert_products 风格）
-- 执行前请确保 user 表中存在 nickname = '商家测试1' 的商家（角色 SELLER）
-- 执行方式：mysql -u root -p overseas_purchase < database/insert_products.sql
-- 或在 MySQL 中：source E:/Programs/legacy/database/insert_products.sql
-- ============================================================

-- 必须：使用 UTF-8，否则中文会报错 Incorrect string value / Cannot convert from gbk to utf8mb4
SET NAMES utf8mb4;

-- 发布者：商家测试1（按昵称查 seller_id）
SET @seller_id = (SELECT id FROM `user` WHERE nickname = '商家测试1' AND deleted = 0 LIMIT 1);
SELECT IF(@seller_id IS NULL, '错误：未找到昵称为【商家测试1】的用户，请先创建该商家。', CONCAT('使用 seller_id = ', @seller_id, ' 插入商品。')) AS 提示;

-- 删除所有现有商品（清空后重新插入）
DELETE FROM `product` WHERE deleted = 0;

-- 电子产品 (category_id = 1)
INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `stock`, `image`, `shipping_address`, `status`) VALUES
(@seller_id, 1, 'Apple iPhone 15 Pro Max 256GB 原封未激活', '全新原封未激活，支持全国联保。钛金属设计，A17 Pro芯片，4800万像素主摄，支持USB-C充电。', 9999.00, 50, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=500', '美国洛杉矶', 'ON_SALE'),
(@seller_id, 1, 'Sony WH-1000XM5 无线降噪耳机 黑色', '索尼旗舰级无线降噪耳机，30小时续航，行业领先降噪技术，舒适轻便设计。', 2299.00, 100, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=500', '日本东京', 'ON_SALE'),
(@seller_id, 1, 'Apple MacBook Air M2 13英寸 256GB', '全新M2芯片，13.6英寸Liquid视网膜显示屏，18小时续航，轻薄便携。', 8499.00, 30, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500', '美国旧金山', 'ON_SALE'),
(@seller_id, 1, 'Nintendo Switch OLED 日版 白色', '任天堂Switch OLED版，7英寸OLED屏幕，64GB存储，支持底座模式。', 2199.00, 80, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=500', '日本大阪', 'ON_SALE'),
(@seller_id, 1, 'Apple AirPods Pro 2代 USB-C版', '主动降噪，自适应通透模式，个性化空间音频，MagSafe充电盒。', 1799.00, 200, 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=500', '美国纽约', 'ON_SALE');

-- 美妆护肤 (category_id = 2)
INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `stock`, `image`, `shipping_address`, `status`) VALUES
(@seller_id, 2, 'SK-II 神仙水 护肤精华露 230ml', '日本本土版SK-II神仙水，富含Pitera精华，改善肤质，提亮肤色。', 1299.00, 150, 'https://images.unsplash.com/photo-1571781926291-c477ebfd024b?w=500', '日本东京', 'ON_SALE'),
(@seller_id, 2, 'La Mer 海蓝之谜 经典面霜 60ml', '奢华修护面霜，深层滋润，改善细纹，恢复肌肤活力。', 2680.00, 50, 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=500', '美国纽约', 'ON_SALE'),
(@seller_id, 2, 'Estee Lauder 雅诗兰黛 小棕瓶精华 100ml', '经典修护精华，夜间密集修护，改善多种肌肤问题。', 899.00, 120, 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500', '法国巴黎', 'ON_SALE'),
(@seller_id, 2, 'CHANEL 香奈儿 蔚蓝男士淡香水 100ml', '经典男士香水，木质馥奇香调，清新持久，彰显品味。', 1050.00, 80, 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=500', '法国巴黎', 'ON_SALE'),
(@seller_id, 2, '资生堂 红腰子精华 75ml', '日本热销精华液，提升肌肤防御力，改善肤色不均。', 680.00, 100, 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=500', '日本东京', 'ON_SALE');

-- 服装鞋帽 (category_id = 3)
INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `stock`, `image`, `shipping_address`, `status`) VALUES
(@seller_id, 3, 'Nike Air Jordan 1 Retro High OG 芝加哥配色', '经典AJ1芝加哥配色，全新正品，带原盒，尺码齐全。', 1899.00, 30, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500', '美国波特兰', 'ON_SALE'),
(@seller_id, 3, 'Adidas Yeezy Boost 350 V2 黑满天星', 'Kanye West联名款，舒适Boost中底，限量配色。', 2499.00, 20, 'https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?w=500', '美国洛杉矶', 'ON_SALE'),
(@seller_id, 3, 'Canada Goose 加拿大鹅 远征派克大衣 黑色', '顶级保暖羽绒服，适合极寒天气，经典款式。', 8999.00, 15, 'https://images.unsplash.com/photo-1539533018447-63fcce2678e3?w=500', '加拿大多伦多', 'ON_SALE'),
(@seller_id, 3, 'Lululemon Align 高腰瑜伽裤 25英寸 黑色', '超柔软Nulu面料，四向弹力，高腰设计，运动休闲皆宜。', 699.00, 100, 'https://images.unsplash.com/photo-1506629082955-511b1aa562c8?w=500', '加拿大温哥华', 'ON_SALE'),
(@seller_id, 3, 'Burberry 经典格纹羊绒围巾 米色', '100%羊绒材质，经典格纹设计，英伦风格。', 3200.00, 40, 'https://images.unsplash.com/photo-1520903920243-00d872a2d1c9?w=500', '英国伦敦', 'ON_SALE');

-- 食品保健 (category_id = 4)
INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `stock`, `image`, `shipping_address`, `status`) VALUES
(@seller_id, 4, 'Swisse 钙+维生素D片 150片', '澳洲热销钙片，添加维生素D3，促进钙吸收，强健骨骼。', 159.00, 300, 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500', '澳大利亚墨尔本', 'ON_SALE'),
(@seller_id, 4, 'Blackmores 深海鱼油软胶囊 400粒', '富含Omega-3，支持心脏、大脑和眼睛健康。', 199.00, 250, 'https://images.unsplash.com/photo-1559757175-5700dde675bc?w=500', '澳大利亚悉尼', 'ON_SALE'),
(@seller_id, 4, '北海道白色恋人巧克力饼干 24枚装', '日本人气伴手礼，白巧克力夹心，入口即化。', 168.00, 200, 'https://images.unsplash.com/photo-1549007994-cb92caebd54b?w=500', '日本北海道', 'ON_SALE'),
(@seller_id, 4, 'ROYCE 生巧克力 原味 20粒', '日本顶级生巧克力，丝滑口感，需冷藏保存。', 128.00, 150, 'https://images.unsplash.com/photo-1481391319762-47dff72954d9?w=500', '日本札幌', 'ON_SALE'),
(@seller_id, 4, 'Manuka Health 麦卢卡蜂蜜 MGO400+ 500g', '新西兰纯天然麦卢卡蜂蜜，高活性因子，养胃护肤。', 458.00, 80, 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=500', '新西兰奥克兰', 'ON_SALE');

-- 母婴用品 (category_id = 5)
INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `stock`, `image`, `shipping_address`, `status`) VALUES
(@seller_id, 5, '花王 Merries 纸尿裤 L码 54片', '日本本土版花王纸尿裤，柔软透气，干爽不漏。', 139.00, 500, 'https://images.unsplash.com/photo-1584839404042-8bc21d240e63?w=500', '日本东京', 'ON_SALE'),
(@seller_id, 5, 'Pigeon 贝亲 宽口径玻璃奶瓶 240ml', '日本贝亲奶瓶，仿母乳实感奶嘴，防胀气设计。', 128.00, 200, 'https://images.unsplash.com/photo-1584839404042-8bc21d240e63?w=500', '日本东京', 'ON_SALE'),
(@seller_id, 5, 'Aptamil 爱他美 白金版奶粉 3段 800g', '德国原装进口，添加天然乳脂，接近母乳配方。', 298.00, 300, 'https://images.unsplash.com/photo-1515942400420-2b98fed1f515?w=500', '德国法兰克福', 'ON_SALE'),
(@seller_id, 5, 'BabyBjorn 婴儿背带 One Air 透气款', '瑞典品牌，人体工学设计，四种背法，透气网眼材质。', 1299.00, 50, 'https://images.unsplash.com/photo-1522771930-78848d9293e8?w=500', '瑞典斯德哥尔摩', 'ON_SALE'),
(@seller_id, 5, 'Stokke Tripp Trapp 成长椅 白色', '挪威设计，可调节高度，从婴儿用到成人。', 2199.00, 30, 'https://images.unsplash.com/photo-1555252333-9f8e92e65df9?w=500', '挪威奥斯陆', 'ON_SALE');

-- 其他 (category_id = 6)
INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `stock`, `image`, `shipping_address`, `status`) VALUES
(@seller_id, 6, 'RIMOWA Original 铝镁合金行李箱 26寸', '德国制造，经典铝镁合金材质，静音万向轮，TSA锁。', 6999.00, 20, 'https://images.unsplash.com/photo-1565026057447-bc90a3dceb87?w=500', '德国科隆', 'ON_SALE'),
(@seller_id, 6, 'Dyson V15 Detect 无线吸尘器', '激光探测灰尘，LCD屏幕显示，60分钟续航。', 4999.00, 40, 'https://images.unsplash.com/photo-1558317374-067fb5f30001?w=500', '英国伦敦', 'ON_SALE'),
(@seller_id, 6, 'MUJI 无印良品 超声波香薰机', '日本简约设计，静音运行，自动断电保护。', 299.00, 150, 'https://images.unsplash.com/photo-1602928321679-560bb453f190?w=500', '日本东京', 'ON_SALE'),
(@seller_id, 6, 'Kindle Paperwhite 5代 8GB 黑色', '6.8英寸E-ink屏幕，300ppi，防水设计，10周续航。', 899.00, 100, 'https://images.unsplash.com/photo-1592496001020-d31bd830651f?w=500', '美国西雅图', 'ON_SALE'),
(@seller_id, 6, 'LEGO 乐高 哈利波特 霍格沃茨城堡 71043', '6020片零件，收藏级巨作，含27个人仔。', 3299.00, 25, 'https://images.unsplash.com/photo-1587654780291-39c9404d746b?w=500', '丹麦比隆', 'ON_SALE');

SELECT ROW_COUNT() AS 插入行数;
SELECT '商品数据已清空并重新插入，发布者：商家测试1。' AS message;
