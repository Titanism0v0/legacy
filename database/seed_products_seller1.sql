-- Batch insert 60 products for pagination test. Default seller_id = 1 (change if needed).
-- To use seller by nickname, run in MySQL client: SET @seller_id = (SELECT id FROM user WHERE nickname = 'xxx' AND deleted = 0 LIMIT 1);
SET @seller_id = 1;

SELECT IF(@seller_id IS NULL, 'No seller found. Set @seller_id manually or create user.', CONCAT('Using seller_id = ', @seller_id)) AS msg;

INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `currency`, `stock`, `shipping_address`, `status`, `view_count`, `deleted`)
SELECT @seller_id, 1, 'Overseas Electronics A001', 'Genuine overseas direct.', 199.00, 'CNY', 50, 'Tokyo', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A002', 'Genuine overseas direct.', 299.00, 'CNY', 60, 'Los Angeles', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A003', 'Genuine overseas direct.', 399.00, 'CNY', 40, 'Seoul', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A004', 'Genuine overseas direct.', 499.00, 'CNY', 30, 'Singapore', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A005', 'Genuine overseas direct.', 599.00, 'CNY', 80, 'Berlin', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A006', 'Genuine overseas direct.', 699.00, 'CNY', 70, 'Osaka', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A007', 'Genuine overseas direct.', 799.00, 'CNY', 55, 'New York', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A008', 'Genuine overseas direct.', 899.00, 'CNY', 45, 'London', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A009', 'Genuine overseas direct.', 1099.00, 'CNY', 35, 'Paris', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 1, 'Overseas Electronics A010', 'Genuine overseas direct.', 1299.00, 'CNY', 25, 'Sydney', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B001', 'Genuine overseas direct.', 88.00, 'CNY', 200, 'Tokyo', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B002', 'Genuine overseas direct.', 128.00, 'CNY', 180, 'Seoul', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B003', 'Genuine overseas direct.', 168.00, 'CNY', 150, 'Paris', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B004', 'Genuine overseas direct.', 198.00, 'CNY', 120, 'Milan', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B005', 'Genuine overseas direct.', 258.00, 'CNY', 100, 'New York', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B006', 'Genuine overseas direct.', 328.00, 'CNY', 90, 'Osaka', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B007', 'Genuine overseas direct.', 398.00, 'CNY', 80, 'London', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B008', 'Genuine overseas direct.', 468.00, 'CNY', 70, 'Berlin', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B009', 'Genuine overseas direct.', 558.00, 'CNY', 60, 'Zurich', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 2, 'Overseas Beauty B010', 'Genuine overseas direct.', 688.00, 'CNY', 50, 'Melbourne', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C001', 'Genuine overseas direct.', 199.00, 'CNY', 100, 'Los Angeles', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C002', 'Genuine overseas direct.', 299.00, 'CNY', 90, 'London', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C003', 'Genuine overseas direct.', 399.00, 'CNY', 80, 'Rome', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C004', 'Genuine overseas direct.', 499.00, 'CNY', 70, 'Tokyo', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C005', 'Genuine overseas direct.', 599.00, 'CNY', 60, 'Paris', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C006', 'Genuine overseas direct.', 699.00, 'CNY', 55, 'Vancouver', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C007', 'Genuine overseas direct.', 899.00, 'CNY', 40, 'Madrid', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C008', 'Genuine overseas direct.', 1099.00, 'CNY', 35, 'Munich', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C009', 'Genuine overseas direct.', 1299.00, 'CNY', 30, 'Chicago', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 3, 'Overseas Fashion C010', 'Genuine overseas direct.', 1599.00, 'CNY', 20, 'Amsterdam', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D001', 'Genuine overseas direct.', 49.00, 'CNY', 300, 'Hokkaido', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D002', 'Genuine overseas direct.', 69.00, 'CNY', 280, 'Sydney', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D003', 'Genuine overseas direct.', 89.00, 'CNY', 260, 'Auckland', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D004', 'Genuine overseas direct.', 109.00, 'CNY', 240, 'California', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D005', 'Genuine overseas direct.', 139.00, 'CNY', 220, 'Munich', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D006', 'Genuine overseas direct.', 169.00, 'CNY', 200, 'Bangkok', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D007', 'Genuine overseas direct.', 199.00, 'CNY', 180, 'Kuala Lumpur', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D008', 'Genuine overseas direct.', 239.00, 'CNY', 160, 'Singapore', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D009', 'Genuine overseas direct.', 289.00, 'CNY', 140, 'Jeju', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 4, 'Overseas Food D010', 'Genuine overseas direct.', 359.00, 'CNY', 120, 'Santiago', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E001', 'Genuine overseas direct.', 99.00, 'CNY', 150, 'Tokyo', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E002', 'Genuine overseas direct.', 129.00, 'CNY', 140, 'Seoul', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E003', 'Genuine overseas direct.', 159.00, 'CNY', 130, 'Frankfurt', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E004', 'Genuine overseas direct.', 199.00, 'CNY', 120, 'New York', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E005', 'Genuine overseas direct.', 249.00, 'CNY', 110, 'London', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E006', 'Genuine overseas direct.', 299.00, 'CNY', 100, 'Rotterdam', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E007', 'Genuine overseas direct.', 369.00, 'CNY', 90, 'Stockholm', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E008', 'Genuine overseas direct.', 449.00, 'CNY', 80, 'Oslo', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E009', 'Genuine overseas direct.', 549.00, 'CNY', 70, 'Copenhagen', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 5, 'Overseas Baby E010', 'Genuine overseas direct.', 699.00, 'CNY', 60, 'Geneva', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F001', 'Genuine overseas direct.', 159.00, 'CNY', 80, 'Tokyo', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F002', 'Genuine overseas direct.', 259.00, 'CNY', 70, 'Seattle', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F003', 'Genuine overseas direct.', 359.00, 'CNY', 60, 'London', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F004', 'Genuine overseas direct.', 459.00, 'CNY', 50, 'Paris', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F005', 'Genuine overseas direct.', 559.00, 'CNY', 45, 'Berlin', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F006', 'Genuine overseas direct.', 659.00, 'CNY', 40, 'Milan', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F007', 'Genuine overseas direct.', 799.00, 'CNY', 35, 'Barcelona', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F008', 'Genuine overseas direct.', 999.00, 'CNY', 30, 'Montreal', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F009', 'Genuine overseas direct.', 1299.00, 'CNY', 25, 'Perth', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL
UNION ALL SELECT @seller_id, 6, 'Overseas Other F010', 'Genuine overseas direct.', 1599.00, 'CNY', 20, 'Wellington', 'ON_SALE', 0, 0 FROM DUAL WHERE @seller_id IS NOT NULL;

SELECT ROW_COUNT() AS rows_inserted;
