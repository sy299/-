CREATE DATABASE IF NOT EXISTS community_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE community_mall;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL, avatar VARCHAR(500), phone VARCHAR(30), role VARCHAR(20) NOT NULL DEFAULT 'USER',
  enabled TINYINT NOT NULL DEFAULT 1, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL, logo VARCHAR(500), show_on_home TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_category_name(name)
);
CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(120) NOT NULL, cover VARCHAR(500) NOT NULL, merchant_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL, price DECIMAL(10,2) NOT NULL, detail TEXT, stock INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
  images JSON, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_merchant FOREIGN KEY (merchant_id) REFERENCES sys_user(id), CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id)
);
CREATE TABLE IF NOT EXISTS shopping_cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, quantity INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cart_user_product(user_id, product_id)
);
CREATE TABLE IF NOT EXISTS address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, receiver_name VARCHAR(50) NOT NULL, receiver_phone VARCHAR(30) NOT NULL,
  detail_address VARCHAR(255) NOT NULL, is_default TINYINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(40) NOT NULL UNIQUE, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL, quantity INT NOT NULL, total_amount DECIMAL(10,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  receiver_name VARCHAR(50) NOT NULL, receiver_phone VARCHAR(30) NOT NULL, receiver_address VARCHAR(255) NOT NULL,
  product_name_snapshot VARCHAR(120), product_cover_snapshot VARCHAR(500), unit_price_snapshot DECIMAL(10,2),
  refund_reason VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, paid_at DATETIME, shipped_at DATETIME, completed_at DATETIME
);
CREATE TABLE IF NOT EXISTS product_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL UNIQUE, product_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
  rating TINYINT NOT NULL, content VARCHAR(1000), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_user_product(user_id, product_id)
);
CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(150) NOT NULL, content TEXT NOT NULL, publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
);
CREATE TABLE IF NOT EXISTS banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, image_url VARCHAR(500) NOT NULL, product_id BIGINT, sort_no INT NOT NULL DEFAULT 0, enabled TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS merchant_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, merchant_id BIGINT NOT NULL, tag VARCHAR(80) NOT NULL, description VARCHAR(255), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO sys_user(id,username,password,nickname,role) VALUES
 (1,'admin','{noop}admin123','平台管理员','ADMIN'),(2,'merchant','{noop}merchant123','示例商家','MERCHANT'),(3,'user','{noop}user123','注册用户','USER');
INSERT IGNORE INTO product_category(id,name,show_on_home) VALUES (1,'新鲜水果',1),(2,'粮油副食',1),(3,'日用百货',1);
INSERT IGNORE INTO product(id,name,cover,merchant_id,category_id,price,detail,stock,status) VALUES
 (1,'陕西红富士苹果','https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=640',2,1,19.90,'脆甜多汁，品质配送。',100,'ON_SALE'),
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
INSERT IGNORE INTO announcement(id,title,content) VALUES (1,'优选商城上线公告','欢迎使用优选商城，平台活动与服务通知将在此发布。');
