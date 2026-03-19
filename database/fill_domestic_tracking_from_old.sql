-- Backfill domestic_tracking_number from legacy tracking_number for existing orders
-- Only fills when domestic_tracking_number is NULL/empty and tracking_number has value.

UPDATE `order`
SET domestic_tracking_number = tracking_number
WHERE (domestic_tracking_number IS NULL OR domestic_tracking_number = '')
  AND tracking_number IS NOT NULL
  AND tracking_number <> ''
  AND deleted = 0;

