import fs from 'node:fs/promises'
import path from 'node:path'

const root = path.resolve(import.meta.dirname, '../..')
const manifest = JSON.parse(await fs.readFile(path.join(import.meta.dirname, 'catalog-manifest.json'), 'utf8'))
  .sort((a, b) => a.productId - b.productId)

if (manifest.length !== 58) throw new Error(`Expected 58 manifest rows, received ${manifest.length}`)

const sqlString = value => `'${String(value ?? '').replaceAll("'", "''")}'`
const sourceCell = value => String(value ?? '').replaceAll('|', '\\|').replaceAll('\n', ' ')

const sourceLines = [
  '# Demo product image sources',
  '',
  'Retrieved on 2026-06-24. Runtime pages use only the local WebP path. Follow each source-page link for the original license and attribution details.',
  '',
  '| Product ID | Product | Local file | Source | Author | License | Source page |',
  '|---:|---|---|---|---|---|---|',
  ...manifest.map(item => `| ${item.productId} | ${sourceCell(item.title)} | \`${sourceCell(item.localPath)}\` | ${sourceCell(item.sourceSite)} | ${sourceCell(item.author)} | [${sourceCell(item.license)}](${item.licenseUrl}) | [original](${item.sourcePage}) |`),
  ''
]

const values = manifest.map(item => `(${item.productId}, ${sqlString(item.title)}, ${sqlString(item.localPath)})`).join(',\n')
const updateSql = `-- 演示商品独立图片映射。默认只检查和备份；显式确认后才更新本地目标库。
SET NAMES utf8mb4;
SET @confirm_demo_product_images = COALESCE(@confirm_demo_product_images, 0);

SELECT DATABASE() AS current_database, @@hostname AS database_host,
       @confirm_demo_product_images AS confirmed,
       '仅允许在已确认的本地 overseas_purchase 库执行' AS warning;

DROP TEMPORARY TABLE IF EXISTS demo_product_image_targets;
CREATE TEMPORARY TABLE demo_product_image_targets (
    product_id BIGINT NOT NULL PRIMARY KEY,
    expected_title VARCHAR(200) NOT NULL,
    new_image VARCHAR(500) NOT NULL
);

INSERT INTO demo_product_image_targets (product_id, expected_title, new_image) VALUES
${values};

SELECT t.product_id, t.expected_title, p.title AS current_title,
       p.image AS old_image, t.new_image, p.status, p.deleted
FROM demo_product_image_targets t
LEFT JOIN product p ON p.id = t.product_id
ORDER BY t.product_id;

SET @target_count = (SELECT COUNT(*) FROM demo_product_image_targets);
SET @mismatch_count = (
    SELECT COUNT(*)
    FROM demo_product_image_targets t
    LEFT JOIN product p ON p.id = t.product_id
    WHERE p.id IS NULL OR p.deleted <> 0 OR p.status <> 'ON_SALE' OR p.title <> t.expected_title
);

CREATE TABLE IF NOT EXISTS demo_product_image_backup_20260624 (
    id BIGINT NOT NULL PRIMARY KEY,
    image VARCHAR(500) NULL,
    images TEXT NULL,
    update_time DATETIME NOT NULL
);

INSERT IGNORE INTO demo_product_image_backup_20260624 (id, image, images, update_time)
SELECT p.id, p.image, p.images, p.update_time
FROM product p
JOIN demo_product_image_targets t ON t.product_id = p.id
WHERE p.deleted = 0 AND p.status = 'ON_SALE' AND p.title = t.expected_title;

SET @backup_count = (SELECT COUNT(*) FROM demo_product_image_backup_20260624 b JOIN demo_product_image_targets t ON t.product_id = b.id);
SET @can_execute_demo_product_images = (
    @confirm_demo_product_images = 1
    AND DATABASE() = 'overseas_purchase'
    AND @target_count = 58
    AND @mismatch_count = 0
    AND @backup_count = 58
);

SELECT @target_count AS target_count, @mismatch_count AS mismatch_count,
       @backup_count AS backup_count, @can_execute_demo_product_images AS can_execute;

START TRANSACTION;

UPDATE product p
JOIN demo_product_image_targets t ON t.product_id = p.id
SET p.image = t.new_image,
    p.images = NULL
WHERE @can_execute_demo_product_images = 1
  AND p.deleted = 0
  AND p.status = 'ON_SALE'
  AND p.title = t.expected_title;

SELECT ROW_COUNT() AS updated_rows;
SELECT COUNT(*) AS mapped_on_sale_products,
       COUNT(DISTINCT image) AS distinct_image_paths
FROM product
WHERE deleted = 0 AND status = 'ON_SALE'
  AND image LIKE '/demo/products/catalog/%.webp';

SET @demo_product_images_finish = IF(@can_execute_demo_product_images = 1, 'COMMIT', 'ROLLBACK');
PREPARE demo_product_images_finish_stmt FROM @demo_product_images_finish;
EXECUTE demo_product_images_finish_stmt;
DEALLOCATE PREPARE demo_product_images_finish_stmt;
`

const rollbackSql = `-- 演示商品图片回滚。仅恢复备份表中的 58 个明确商品。
SET NAMES utf8mb4;
SET @confirm_demo_product_images_rollback = COALESCE(@confirm_demo_product_images_rollback, 0);

SELECT DATABASE() AS current_database,
       @confirm_demo_product_images_rollback AS confirmed,
       COUNT(*) AS backup_count
FROM demo_product_image_backup_20260624;

SET @can_rollback_demo_product_images = (
    @confirm_demo_product_images_rollback = 1
    AND DATABASE() = 'overseas_purchase'
    AND (SELECT COUNT(*) FROM demo_product_image_backup_20260624) = 58
);

START TRANSACTION;

UPDATE product p
JOIN demo_product_image_backup_20260624 b ON b.id = p.id
SET p.image = b.image,
    p.images = b.images,
    p.update_time = b.update_time
WHERE @can_rollback_demo_product_images = 1;

SELECT ROW_COUNT() AS restored_rows;

SET @demo_product_images_rollback_finish = IF(@can_rollback_demo_product_images = 1, 'COMMIT', 'ROLLBACK');
PREPARE demo_product_images_rollback_finish_stmt FROM @demo_product_images_rollback_finish;
EXECUTE demo_product_images_rollback_finish_stmt;
DEALLOCATE PREPARE demo_product_images_rollback_finish_stmt;
`

const sourcePath = path.join(root, 'frontend/public/demo/products/catalog/SOURCES.md')
const updatePath = path.join(root, 'database/demo_product_images_20260624.sql')
const rollbackPath = path.join(root, 'database/demo_product_images_20260624_rollback.sql')
await fs.writeFile(sourcePath, sourceLines.join('\n'), 'utf8')
await fs.writeFile(updatePath, updateSql, 'utf8')
await fs.writeFile(rollbackPath, rollbackSql, 'utf8')
console.log(`Generated ${manifest.length} source rows and SQL targets`)
