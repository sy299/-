USE community_mall;

INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 2, p.id, CONCAT(p.name, '限时秒杀'), 39.90, 18, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 2;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 3, p.id, CONCAT(p.name, '限时秒杀'), 19.90, 24, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 3;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 4, p.id, CONCAT(p.name, '限时秒杀'), 32.90, 16, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 4;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 5, p.id, CONCAT(p.name, '限时秒杀'), 39.90, 30, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 5;
INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 6, p.id, CONCAT(p.name, '限时秒杀'), 59.90, 14, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p WHERE p.id = 6;
