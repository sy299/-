USE community_mall;
SET NAMES utf8mb4;

-- Demo catalog used by the storefront and merchant workspace.
INSERT IGNORE INTO product(id,name,cover,merchant_id,category_id,price,detail,stock,status) VALUES
 (2,'海南金枕榴莲','https://images.unsplash.com/photo-1559181567-c3190ca9959b?w=640',2,1,59.90,'果肉饱满细腻，冷链新鲜到家。',36,'ON_SALE'),
 (3,'云南高原蓝莓','https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=640',2,1,29.90,'高原鲜采，酸甜可口，颗粒饱满。',80,'ON_SALE'),
 (4,'智利进口车厘子','https://images.unsplash.com/photo-1528825871115-3581a5387919?w=640',2,1,49.90,'果大色泽红润，甜度在线。',60,'ON_SALE'),
 (5,'五常生态大米 5kg','https://images.unsplash.com/photo-1586201375761-83865001e31c?w=640',2,2,49.80,'粒粒饱满，米香浓郁，家常好米。',200,'ON_SALE'),
 (6,'初榨橄榄油 750ml','https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=640',2,2,79.00,'低温初榨，清新果香，日常烹饪适用。',80,'ON_SALE'),
 (7,'手工全麦面包','https://images.unsplash.com/photo-1509440159596-0249088772ff?w=640',2,2,22.80,'全麦谷物制作，松软有嚼劲。',100,'ON_SALE'),
 (8,'高钙纯牛奶 250ml*12','https://images.unsplash.com/photo-1550583724-b2692b85b150?w=640',2,2,39.90,'营养早餐搭档，醇厚顺滑。',120,'ON_SALE'),
 (9,'植物洗衣凝珠 30颗','https://images.unsplash.com/photo-1583947215259-38e31be8751f?w=640',2,3,36.90,'一颗一桶，洁净清香，方便省心。',90,'ON_SALE'),
 (10,'棉柔抽纸 24包','https://images.unsplash.com/photo-1583947215259-38e31be8751f?w=640',2,3,19.90,'原生木浆，柔韧亲肤，家庭常备。',160,'ON_SALE'),
 (11,'轻便保温杯 500ml','https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=640',2,3,59.00,'食品级内胆，长效保温，通勤便携。',75,'ON_SALE'),
 (12,'简约护眼台灯','https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=640',2,3,89.00,'柔和护眼光线，书桌学习好搭档。',45,'ON_SALE'),
 (13,'双肩通勤背包','https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=640',2,3,119.00,'轻量防泼水，多分区收纳设计。',50,'ON_SALE');
