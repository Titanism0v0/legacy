-- Seed: real products for seller s_test, each with 2-3 images from uploads/product/ (use existing local files only).
-- Run: mysql -u root -p overseas_purchase < database/seed_products_s_test.sql
-- Or in MySQL: source E:/Programs/legacy/database/seed_products_s_test.sql

SET @seller_id = (SELECT id FROM `user` WHERE username = 's_test' AND deleted = 0 LIMIT 1);
SELECT IF(@seller_id IS NULL, 'ERROR: Create user s_test first.', CONCAT('Using seller_id = ', @seller_id)) AS msg;
DELETE FROM `product` WHERE deleted = 0 AND @seller_id IS NOT NULL;

-- Use only existing files in uploads/product/: 9 old + p01_1.jpg (no external download needed)
SET @a = '/api/upload/product/20260307130708453810.jpg';
SET @b = '/api/upload/product/202603071307335aae87.jpg';
SET @c = '/api/upload/product/20260307130735c60bcb.jpg';
SET @d = '/api/upload/product/2026030811464022199d.jpg';
SET @e = '/api/upload/product/202603081146422b2224.jpg';
SET @f = '/api/upload/product/20260308120840a0cf63.jpg';
SET @g = '/api/upload/product/202603081208451c6ae1.jpg';
SET @h = '/api/upload/product/202603081306169b73b9.jpg';
SET @i = '/api/upload/product/202603081306184ac0ff.jpg';
SET @j = '/api/upload/product/p01_1.jpg';

INSERT INTO `product` (`seller_id`, `category_id`, `title`, `description`, `price`, `currency`, `stock`, `image`, `images`, `shipping_address`, `status`, `view_count`, `deleted`) VALUES
(@seller_id, 1, 'Sony WH-1000XM5 Wireless Noise Cancelling Headphones', 'Industry-leading noise cancellation, 30hr battery, multipoint connection. Black.', 2299.00, 'CNY', 50, @a, CONCAT('["', @a, '","', @b, '","', @c, '"]'), 'Tokyo, Japan', 'ON_SALE', 0, 0),
(@seller_id, 1, 'Apple AirPods Pro 2nd Gen USB-C', 'Active noise cancellation, Adaptive Audio, MagSafe charging case.', 1799.00, 'CNY', 80, @b, CONCAT('["', @b, '","', @c, '","', @d, '"]'), 'New York, USA', 'ON_SALE', 0, 0),
(@seller_id, 1, 'Samsung Galaxy Watch 6 Classic 47mm', 'Smartwatch with rotating bezel, health monitoring, 40hr battery. LTE optional.', 2499.00, 'CNY', 40, @c, CONCAT('["', @c, '","', @d, '"]'), 'Seoul, Korea', 'ON_SALE', 0, 0),
(@seller_id, 1, 'Kindle Paperwhite 5 6.8" 8GB', 'E-ink display, waterproof, weeks of battery. Perfect for reading.', 899.00, 'CNY', 100, @d, CONCAT('["', @d, '","', @e, '"]'), 'Seattle, USA', 'ON_SALE', 0, 0),
(@seller_id, 1, 'Anker PowerCore 20000mAh PD', 'High-capacity power bank, 20W PD fast charge, dual USB. Compact design.', 299.00, 'CNY', 200, @e, CONCAT('["', @e, '","', @f, '","', @g, '"]'), 'Shenzhen, China', 'ON_SALE', 0, 0),

(@seller_id, 2, 'La Mer Crème de la Mer Moisturizing Cream 60ml', 'Luxury moisturizer, deep hydration, smooths fine lines. Original formula.', 2680.00, 'CNY', 30, @f, CONCAT('["', @f, '","', @g, '"]'), 'New York, USA', 'ON_SALE', 0, 0),
(@seller_id, 2, 'Estée Lauder Advanced Night Repair Serum 100ml', 'Iconic repair serum, overnight renewal. For all skin types.', 899.00, 'CNY', 120, @g, CONCAT('["', @g, '","', @h, '"]'), 'Paris, France', 'ON_SALE', 0, 0),
(@seller_id, 2, 'SK-II Facial Treatment Essence 230ml', 'Pitera essence, improves texture and radiance. Japanese domestic version.', 1299.00, 'CNY', 80, @h, CONCAT('["', @h, '","', @i, '"]'), 'Tokyo, Japan', 'ON_SALE', 0, 0),
(@seller_id, 2, 'Chanel Coco Mademoiselle EDP 100ml', 'Fresh oriental fragrance, long-lasting. Women.', 1050.00, 'CNY', 60, @i, CONCAT('["', @i, '","', @j, '"]'), 'Paris, France', 'ON_SALE', 0, 0),
(@seller_id, 2, 'Shiseido Ultimune Power Infusing Concentrate 75ml', 'Boosts skin defense, evens tone. Bestseller in Japan.', 680.00, 'CNY', 90, @j, CONCAT('["', @j, '","', @a, '"]'), 'Tokyo, Japan', 'ON_SALE', 0, 0),

(@seller_id, 3, 'Nike Air Jordan 1 Retro High OG Chicago', 'Classic AJ1 Chicago colorway. Brand new with box. Full size run.', 1899.00, 'CNY', 25, @a, CONCAT('["', @a, '","', @b, '","', @c, '"]'), 'Portland, USA', 'ON_SALE', 0, 0),
(@seller_id, 3, 'Canada Goose Expedition Parka Black', 'Extreme weather parka. Authentic. Rare size available.', 8999.00, 'CNY', 10, @b, CONCAT('["', @b, '","', @c, '"]'), 'Toronto, Canada', 'ON_SALE', 0, 0),
(@seller_id, 3, 'Lululemon Align High-Rise Pant 25" Black', 'Nulu fabric, four-way stretch. Size 2-10. Ship from Vancouver.', 699.00, 'CNY', 70, @c, CONCAT('["', @c, '","', @d, '"]'), 'Vancouver, Canada', 'ON_SALE', 0, 0),
(@seller_id, 3, 'Burberry Classic Check Cashmere Scarf', '100% cashmere. Iconic check. Beige. Gift box.', 3200.00, 'CNY', 35, @d, CONCAT('["', @d, '","', @e, '"]'), 'London, UK', 'ON_SALE', 0, 0),
(@seller_id, 3, 'Adidas Yeezy Boost 350 V2 Zebra', 'Comfortable Boost midsole. Limited restock. Authentic.', 1999.00, 'CNY', 20, @e, CONCAT('["', @e, '","', @f, '"]'), 'Los Angeles, USA', 'ON_SALE', 0, 0),

(@seller_id, 4, 'Swisse Calcium + Vitamin D 150 Tablets', 'Australian bestseller. Supports bone health. Easy to swallow.', 159.00, 'CNY', 300, @f, CONCAT('["', @f, '","', @g, '"]'), 'Melbourne, Australia', 'ON_SALE', 0, 0),
(@seller_id, 4, 'Blackmores Fish Oil 1000 400 Capsules', 'Omega-3. Heart, brain, eye support. No fishy aftertaste.', 199.00, 'CNY', 250, @g, CONCAT('["', @g, '","', @h, '"]'), 'Sydney, Australia', 'ON_SALE', 0, 0),
(@seller_id, 4, 'Hokkaido Shiroi Koibito Cookie 24pcs', 'Famous white chocolate sandwich cookie. Must-try from Hokkaido.', 168.00, 'CNY', 180, @h, CONCAT('["', @h, '","', @i, '"]'), 'Hokkaido, Japan', 'ON_SALE', 0, 0),
(@seller_id, 4, 'ROYCE Nama Chocolate Original 20pcs', 'Premium raw chocolate. Keep refrigerated. Best before 30 days.', 128.00, 'CNY', 150, @i, CONCAT('["', @i, '","', @j, '"]'), 'Sapporo, Japan', 'ON_SALE', 0, 0),
(@seller_id, 4, 'Manuka Health MGO 400+ Honey 500g', 'New Zealand manuka honey. High activity. Good for throat and skin.', 458.00, 'CNY', 60, @j, CONCAT('["', @j, '","', @a, '"]'), 'Auckland, NZ', 'ON_SALE', 0, 0),

(@seller_id, 5, 'Merries Diapers L 54pcs Japan Version', 'Soft, breathable. Japanese domestic. Popular for baby skin.', 139.00, 'CNY', 400, @a, CONCAT('["', @a, '","', @b, '"]'), 'Tokyo, Japan', 'ON_SALE', 0, 0),
(@seller_id, 5, 'Pigeon Wide Neck Glass Bottle 240ml', 'Japanese Pigeon. Breast-like nipple. Anti-colic. Easy to clean.', 128.00, 'CNY', 150, @b, CONCAT('["', @b, '","', @c, '"]'), 'Tokyo, Japan', 'ON_SALE', 0, 0),
(@seller_id, 5, 'Aptamil Gold+ Stage 3 Formula 800g', 'German origin. Closer to breast milk. Popular choice.', 298.00, 'CNY', 200, @c, CONCAT('["', @c, '","', @d, '"]'), 'Frankfurt, Germany', 'ON_SALE', 0, 0),
(@seller_id, 5, 'BabyBjorn One Air Baby Carrier', 'Ergonomic, four carrying positions. Mesh breathable. From Sweden.', 1299.00, 'CNY', 45, @d, CONCAT('["', @d, '","', @e, '"]'), 'Stockholm, Sweden', 'ON_SALE', 0, 0),
(@seller_id, 5, 'Stokke Tripp Trapp Chair White', 'Grows with child. Adjustable. Nordic design. From Norway.', 2199.00, 'CNY', 25, @e, CONCAT('["', @e, '","', @f, '"]'), 'Oslo, Norway', 'ON_SALE', 0, 0),

(@seller_id, 6, 'RIMOWA Original Cabin 21" Aluminum', 'German-made. Lightweight. TSA lock. Lifetime quality.', 6999.00, 'CNY', 15, @f, CONCAT('["', @f, '","', @g, '"]'), 'Cologne, Germany', 'ON_SALE', 0, 0),
(@seller_id, 6, 'Dyson V15 Detect Cordless Vacuum', 'Laser dust detection. LCD. 60min runtime. Full kit.', 4999.00, 'CNY', 30, @g, CONCAT('["', @g, '","', @h, '"]'), 'London, UK', 'ON_SALE', 0, 0),
(@seller_id, 6, 'MUJI Ultrasonic Aroma Diffuser', 'Minimal design. Quiet. Auto shut-off. Japan.', 299.00, 'CNY', 120, @h, CONCAT('["', @h, '","', @i, '"]'), 'Tokyo, Japan', 'ON_SALE', 0, 0),
(@seller_id, 6, 'LEGO Harry Potter Hogwarts Castle 71043', '6020 pieces. 27 minifigures. Collector set. From Denmark.', 3299.00, 'CNY', 20, @i, CONCAT('["', @i, '","', @j, '"]'), 'Billund, Denmark', 'ON_SALE', 0, 0),
(@seller_id, 6, 'Nintendo Switch OLED White Japan', '7" OLED screen. 64GB. Dock mode. Region-free for cartridges.', 2199.00, 'CNY', 55, @j, CONCAT('["', @j, '","', @a, '","', @b, '"]'), 'Osaka, Japan', 'ON_SALE', 0, 0);

SELECT ROW_COUNT() AS rows_inserted;
SELECT 'Seed done. All images from uploads/product/ (no download script needed).' AS message;
