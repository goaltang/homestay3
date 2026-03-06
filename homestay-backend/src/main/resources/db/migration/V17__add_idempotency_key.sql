-- V17__add_idempotency_key.sql
-- 添加幂等性 key 字段，用于防止重复创建订单

DELIMITER //

-- 1. 添加幂等性 key 字段
CREATE PROCEDURE add_idempotency_key_column()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'orders' 
                   AND COLUMN_NAME = 'idempotency_key') THEN
        ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(64) NULL;
    END IF;
END //

-- 2. 添加唯一索引
CREATE PROCEDURE create_idempotency_unique_index()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'orders' 
                   AND INDEX_NAME = 'idx_orders_idempotency_key') THEN
        CREATE UNIQUE INDEX idx_orders_idempotency_key ON orders (idempotency_key);
    END IF;
END //

-- 3. 添加复合索引
CREATE PROCEDURE create_guest_idempotency_index()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'orders' 
                   AND INDEX_NAME = 'idx_orders_guest_idempotency') THEN
        CREATE INDEX idx_orders_guest_idempotency ON orders (guest_id, idempotency_key);
    END IF;
END //

DELIMITER ;

-- 执行存储过程
CALL add_idempotency_key_column();
CALL create_idempotency_unique_index();
CALL create_guest_idempotency_index();

-- 删除存储过程
DROP PROCEDURE IF EXISTS add_idempotency_key_column;
DROP PROCEDURE IF EXISTS create_idempotency_unique_index;
DROP PROCEDURE IF EXISTS create_guest_idempotency_index;
