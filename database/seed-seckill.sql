USE community_mall;

INSERT IGNORE INTO seckill_activity(id, product_id, title, seckill_price, stock, start_time, end_time, status)
SELECT 1, p.id, CONCAT(p.name, '限时秒杀'), 9.90, 20, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), 'PUBLISHED'
FROM product p
WHERE p.id = 1;
