-- 演示数据清理（仅供人工审查后在指定的非生产/授权数据库执行）
-- 默认 @confirm_demo_cleanup=0，因此直接运行只会生成候选清单和备份表，不会更新数据。
-- 执行人必须先核对 DATABASE()、候选记录和备份数量，再在当前会话设置为 1。
-- 本脚本不删除任何商品，不修改历史初始化脚本，不连接任何远程数据库。

SET NAMES utf8mb4;
SET @confirm_demo_cleanup = COALESCE(@confirm_demo_cleanup, 0);

SELECT DATABASE() AS current_database,
       @@hostname AS database_host,
       '确认不是生产库后，才可将 @confirm_demo_cleanup 改为 1' AS warning;

DROP TEMPORARY TABLE IF EXISTS demo_cleanup_targets;
CREATE TEMPORARY TABLE demo_cleanup_targets (
    product_id BIGINT NOT NULL PRIMARY KEY,
    reason VARCHAR(255) NOT NULL,
    category_seq INT NULL
);

-- 明确来源一：data.sql 中“测试商品-类别NNN”或 picsum 随机图片记录。
INSERT INTO demo_cleanup_targets (product_id, reason, category_seq)
SELECT p.id,
       'seed_test_product',
       ROW_NUMBER() OVER (PARTITION BY p.category_id ORDER BY p.id)
FROM product p
WHERE p.deleted = 0
  AND (
      p.title REGEXP '^测试商品-(电子|美妆|服装|食品|母婴|其他)[0-9]{3}$'
      OR p.image LIKE 'https://picsum.photos/%'
  );

-- 明确来源二：空值、测试值或占位值发货地；只加入候选，不做全表更新。
INSERT INTO demo_cleanup_targets (product_id, reason, category_seq)
SELECT p.id, 'invalid_shipping_address', NULL
FROM product p
WHERE p.deleted = 0
  AND (
      p.shipping_address IS NULL
      OR TRIM(p.shipping_address) = ''
      OR LOWER(TRIM(p.shipping_address)) IN ('test', 'null', 'undefined', 'unknown')
      OR p.shipping_address LIKE '%测试地址%'
  )
ON DUPLICATE KEY UPDATE reason = CONCAT(demo_cleanup_targets.reason, ',invalid_shipping_address');

-- 明确来源三：标题/描述中出现高风险违禁品词。命中项仅下架并改为安全展示文本。
INSERT INTO demo_cleanup_targets (product_id, reason, category_seq)
SELECT p.id, 'prohibited_content', NULL
FROM product p
WHERE p.deleted = 0
  AND CONCAT_WS(' ', p.title, p.description) REGEXP
      '(海洛因|冰毒|可卡因|芬太尼|毒品|枪支|手枪|步枪|炸药|雷管|色情|赌博|博彩|heroin|cocaine|fentanyl|porn|gambling|casino)'
ON DUPLICATE KEY UPDATE reason = CONCAT(demo_cleanup_targets.reason, ',prohibited_content');

-- 明确来源四：历史演示脚本中的 Unsplash/Picsum 外链，统一替换为项目本地资源。
INSERT INTO demo_cleanup_targets (product_id, reason, category_seq)
SELECT p.id, 'external_demo_image', NULL
FROM product p
WHERE p.deleted = 0
  AND (
      p.image LIKE 'https://images.unsplash.com/%'
      OR p.image LIKE 'https://picsum.photos/%'
  )
ON DUPLICATE KEY UPDATE reason = CONCAT(demo_cleanup_targets.reason, ',external_demo_image');

-- 执行前检查：必须人工核对每条记录。
SELECT t.reason, p.id, p.seller_id, p.category_id, p.title, p.description,
       p.price, p.stock, p.shipping_address, p.image, p.status
FROM demo_cleanup_targets t
JOIN product p ON p.id = t.product_id
ORDER BY p.category_id, p.id;

SELECT reason, COUNT(*) AS affected_count
FROM demo_cleanup_targets
GROUP BY reason
ORDER BY reason;

-- 持久化逐行备份，供 demo_data_cleanup_20260624_rollback.sql 回滚。
CREATE TABLE IF NOT EXISTS demo_cleanup_product_backup_20260624 LIKE product;
INSERT IGNORE INTO demo_cleanup_product_backup_20260624
SELECT p.*
FROM product p
JOIN demo_cleanup_targets t ON t.product_id = p.id;

SELECT COUNT(*) AS target_count FROM demo_cleanup_targets;
SELECT COUNT(*) AS backup_count FROM demo_cleanup_product_backup_20260624;

START TRANSACTION;

-- 将明确的测试商品转换为六分类、每类最多五条的稳定演示商品。
UPDATE product p
JOIN demo_cleanup_targets t ON t.product_id = p.id
SET p.title = CASE p.category_id
        WHEN 1 THEN ELT(LEAST(COALESCE(t.category_seq, 1), 5), '便携蓝牙降噪耳机', '旅行多口充电器', '轻量电子阅读器', '桌面机械键盘', '便携照片打印机')
        WHEN 2 THEN ELT(LEAST(COALESCE(t.category_seq, 1), 5), '温和氨基酸洁面乳', '保湿修护精华液', '轻盈防晒乳', '植物香氛护手霜', '旅行化妆刷套装')
        WHEN 3 THEN ELT(LEAST(COALESCE(t.category_seq, 1), 5), '轻量防风夹克', '舒适慢跑运动鞋', '羊毛混纺围巾', '通勤帆布托特包', '速干运动长裤')
        WHEN 4 THEN ELT(LEAST(COALESCE(t.category_seq, 1), 5), '烘焙燕麦脆片', '低糖黑巧克力礼盒', '原味坚果组合', '冻干草莓脆', '手冲咖啡豆')
        WHEN 5 THEN ELT(LEAST(COALESCE(t.category_seq, 1), 5), '婴童柔软浴巾', '宽口径玻璃奶瓶', '便携辅食餐具', '婴儿透气背带', '儿童安全水杯')
        ELSE ELT(LEAST(COALESCE(t.category_seq, 1), 5), '轻便旅行收纳箱', '桌面香薰加湿器', '可折叠阅读灯', '积木创意套装', '便携保温杯')
    END,
    p.description = CASE p.category_id
        WHEN 1 THEN '适合通勤与旅行的实用数码用品，参数和库存仅用于功能演示。'
        WHEN 2 THEN '温和日常护理用品，展示商品搜索、详情与分类筛选功能。'
        WHEN 3 THEN '舒适耐用的日常服饰单品，尺码信息以演示页面为准。'
        WHEN 4 THEN '正规包装的日常食品，配料与保质期信息仅作演示。'
        WHEN 5 THEN '适合家庭日常使用的母婴用品，页面信息仅作功能演示。'
        ELSE '精选生活方式用品，用于展示分页、推荐与商品详情功能。'
    END,
    p.price = CASE p.category_id
        WHEN 1 THEN 199.00 + COALESCE(t.category_seq, 1) * 60
        WHEN 2 THEN 79.00 + COALESCE(t.category_seq, 1) * 30
        WHEN 3 THEN 169.00 + COALESCE(t.category_seq, 1) * 70
        WHEN 4 THEN 39.00 + COALESCE(t.category_seq, 1) * 20
        WHEN 5 THEN 89.00 + COALESCE(t.category_seq, 1) * 35
        ELSE 119.00 + COALESCE(t.category_seq, 1) * 45
    END,
    p.stock = 20 + COALESCE(t.category_seq, 1) * 12,
    p.shipping_address = CASE p.category_id
        WHEN 1 THEN '日本东京演示仓'
        WHEN 2 THEN '法国巴黎演示仓'
        WHEN 3 THEN '英国伦敦演示仓'
        WHEN 4 THEN '澳大利亚悉尼演示仓'
        WHEN 5 THEN '德国法兰克福演示仓'
        ELSE '新加坡演示仓'
    END,
    p.image = CASE p.category_id
        WHEN 1 THEN '/demo/products/electronics.svg'
        WHEN 2 THEN '/demo/products/beauty.svg'
        WHEN 3 THEN '/demo/products/apparel.svg'
        WHEN 4 THEN '/demo/products/food.svg'
        WHEN 5 THEN '/demo/products/baby.svg'
        ELSE '/demo/products/lifestyle.svg'
    END,
    p.images = NULL,
    p.status = 'ON_SALE'
WHERE @confirm_demo_cleanup = 1
  AND t.reason LIKE '%seed_test_product%';

UPDATE product p
JOIN demo_cleanup_targets t ON t.product_id = p.id
SET p.image = CASE p.category_id
        WHEN 1 THEN '/demo/products/electronics.svg'
        WHEN 2 THEN '/demo/products/beauty.svg'
        WHEN 3 THEN '/demo/products/apparel.svg'
        WHEN 4 THEN '/demo/products/food.svg'
        WHEN 5 THEN '/demo/products/baby.svg'
        ELSE '/demo/products/lifestyle.svg'
    END,
    p.images = NULL
WHERE @confirm_demo_cleanup = 1
  AND t.reason LIKE '%external_demo_image%';

UPDATE product p
JOIN demo_cleanup_targets t ON t.product_id = p.id
SET p.shipping_address = '新加坡演示仓'
WHERE @confirm_demo_cleanup = 1
  AND t.reason LIKE '%invalid_shipping_address%'
  AND t.reason NOT LIKE '%seed_test_product%';

UPDATE product p
JOIN demo_cleanup_targets t ON t.product_id = p.id
SET p.title = CONCAT('已下架演示内容 #', p.id),
    p.description = '该记录因命中平台禁限售规则已下架，仅保留用于审核流程演示。',
    p.image = '/placeholder.svg',
    p.images = NULL,
    p.status = 'OFF_SALE'
WHERE @confirm_demo_cleanup = 1
  AND t.reason LIKE '%prohibited_content%';

-- 受影响记录确认；数量或内容不符时执行 ROLLBACK，不要 COMMIT。
SELECT ROW_COUNT() AS last_statement_affected_rows;
SELECT p.category_id, p.status, COUNT(*) AS product_count
FROM product p
JOIN demo_cleanup_targets t ON t.product_id = p.id
GROUP BY p.category_id, p.status
ORDER BY p.category_id, p.status;

SELECT p.id, p.category_id, p.title, p.price, p.stock, p.shipping_address, p.image, p.status
FROM product p
JOIN demo_cleanup_targets t ON t.product_id = p.id
ORDER BY p.category_id, p.id;

-- 只有调用方在当前会话显式设置确认变量时才提交，否则回滚。
SET @demo_cleanup_finish = IF(@confirm_demo_cleanup = 1, 'COMMIT', 'ROLLBACK');
PREPARE demo_cleanup_finish_stmt FROM @demo_cleanup_finish;
EXECUTE demo_cleanup_finish_stmt;
DEALLOCATE PREPARE demo_cleanup_finish_stmt;

-- 真正执行方式：核对候选与备份后，在同一 MySQL 会话执行：
-- SET @confirm_demo_cleanup=1;
-- SOURCE database/demo_data_cleanup_20260624.sql;
-- 不要在生产库直接执行。
