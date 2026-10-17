USE community_mall;

INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 7, p.id, CONCAT(p.name, '限时秒杀'), 19.90, 24, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 3 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 8, p.id, CONCAT(p.name, '限时秒杀'), 14.90, 20, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 7 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 9, p.id, CONCAT(p.name, '限时秒杀'), 29.90, 30, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 8 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 10, p.id, CONCAT(p.name, '限时秒杀'), 24.90, 25, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 9 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 11, p.id, CONCAT(p.name, '限时秒杀'), 12.90, 40, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 10 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 12, p.id, CONCAT(p.name, '限时秒杀'), 39.90, 20, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 11 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 13, p.id, CONCAT(p.name, '限时秒杀'), 59.90, 15, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 12 AND p.status = 'ON_SALE' AND p.stock > 0;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 14, p.id, CONCAT(p.name, '限时秒杀'), 79.90, 18, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 13 AND p.status = 'ON_SALE' AND p.stock > 0;
