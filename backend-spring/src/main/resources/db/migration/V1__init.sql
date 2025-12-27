CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(32) NOT NULL,
  password_hash VARCHAR(60) NOT NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS seckill_goods (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  description TEXT NULL,
  stock INT NULL,
  original_price DECIMAL(10,2) NOT NULL,
  seckill_price DECIMAL(10,2) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL
);

CREATE INDEX idx_seckill_goods_start_time ON seckill_goods (start_time);
CREATE INDEX idx_seckill_goods_end_time ON seckill_goods (end_time);

CREATE TABLE IF NOT EXISTS seckill_orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  goods_id BIGINT NOT NULL,
  create_time DATETIME NULL,
  status INT NULL,
  updated_at DATETIME NULL,
  external_order_id VARCHAR(64) NULL,
  UNIQUE KEY uk_seckill_orders_external_order_id (external_order_id)
);

CREATE INDEX idx_seckill_orders_user_status ON seckill_orders (user_id, status);
CREATE INDEX idx_seckill_orders_goods ON seckill_orders (goods_id);
CREATE INDEX idx_seckill_orders_create_time ON seckill_orders (create_time);
