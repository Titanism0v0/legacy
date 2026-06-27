-- 演示数据清理回滚脚本。仅恢复备份表中明确记录，不做全表更新/删除。
SET NAMES utf8mb4;
SET @confirm_demo_cleanup_rollback = COALESCE(@confirm_demo_cleanup_rollback, 0);

SELECT DATABASE() AS current_database,
       '确认目标库与备份表后，才可将 @confirm_demo_cleanup_rollback 改为 1' AS warning;

SELECT COUNT(*) AS backup_count FROM demo_cleanup_product_backup_20260624;
SELECT id, title, price, stock, shipping_address, image, status
FROM demo_cleanup_product_backup_20260624
ORDER BY id;

START TRANSACTION;

UPDATE product p
JOIN demo_cleanup_product_backup_20260624 b ON b.id = p.id
SET p.seller_id = b.seller_id,
    p.category_id = b.category_id,
    p.title = b.title,
    p.description = b.description,
    p.price = b.price,
    p.currency = b.currency,
    p.stock = b.stock,
    p.image = b.image,
    p.images = b.images,
    p.shipping_address = b.shipping_address,
    p.status = b.status,
    p.view_count = b.view_count,
    p.create_time = b.create_time,
    p.update_time = b.update_time,
    p.deleted = b.deleted
WHERE @confirm_demo_cleanup_rollback = 1;

SELECT ROW_COUNT() AS restored_rows;
SELECT p.id, p.title, p.shipping_address, p.image, p.status
FROM product p
JOIN demo_cleanup_product_backup_20260624 b ON b.id = p.id
ORDER BY p.id;

SET @demo_cleanup_rollback_finish = IF(@confirm_demo_cleanup_rollback = 1, 'COMMIT', 'ROLLBACK');
PREPARE demo_cleanup_rollback_finish_stmt FROM @demo_cleanup_rollback_finish;
EXECUTE demo_cleanup_rollback_finish_stmt;
DEALLOCATE PREPARE demo_cleanup_rollback_finish_stmt;

-- 真正回滚方式（同一 MySQL 会话）：
-- SET @confirm_demo_cleanup_rollback=1;
-- SOURCE database/demo_data_cleanup_20260624_rollback.sql;
