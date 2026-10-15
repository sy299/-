USE community_mall;

CREATE TABLE IF NOT EXISTS seckill_activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  seckill_price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_seckill_status_window(status, start_time, end_time),
  CONSTRAINT fk_seckill_activity_product FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT chk_seckill_price CHECK (seckill_price >= 0),
  CONSTRAINT chk_seckill_stock CHECK (stock >= 0),
  CONSTRAINT chk_seckill_window CHECK (end_time > start_time)
);

CREATE TABLE IF NOT EXISTS seckill_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  order_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_seckill_activity_user(activity_id, user_id),
  CONSTRAINT fk_seckill_order_activity FOREIGN KEY (activity_id) REFERENCES seckill_activity(id),
  CONSTRAINT fk_seckill_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_seckill_order_product FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT fk_seckill_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
