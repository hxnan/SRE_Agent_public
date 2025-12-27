INSERT INTO users (username, password_hash, created_at, updated_at)
VALUES
  ('testuser', '$2a$10$eXPk.9oW/goFoqqznrsSJePpWagNfQ9QbTPqYywGxSch5EycgiyCq', NOW(), NOW()),
  ('admin', '$2a$10$VtngRU/iH7ZPlMGINr9eN.tYbgWsngNNG5jDHIpwA4slxvjPfDL2O', NOW(), NOW())
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), updated_at = NOW();

INSERT INTO seckill_goods (name, description, stock, original_price, seckill_price, start_time, end_time, created_at, updated_at)
VALUES
  ('iPhone 15 Pro', 'A17 Pro 芯片，钛金属机身', 50, 8999.00, 6999.00, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 24 HOUR), NOW(), NOW()),
  ('Xiaomi 14 Pro', '徕卡光学，第二代骁龙8', 80, 4999.00, 3999.00, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 12 HOUR), NOW(), NOW()),
  ('Huawei Mate 60', '卫星通信，鸿蒙生态', 60, 6999.00, 5999.00, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 24 HOUR), NOW(), NOW()),
  ('OnePlus 12', '高刷屏，长寿命电池', 70, 5999.00, 4599.00, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 26 HOUR), NOW(), NOW());
