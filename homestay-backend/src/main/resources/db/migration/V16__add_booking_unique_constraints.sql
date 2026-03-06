-- V16__add_booking_unique_constraints.sql
-- 防止同一房源在同一日期范围内被重复预订的唯一约束

-- 使用存储过程处理索引创建（MySQL 不支持 CREATE INDEX IF NOT EXISTS）

DELIMITER //

-- 1. 添加复合索引用于查询优化
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'orders' 
                   AND INDEX_NAME = 'idx_orders_homestay_checkin') THEN
        CREATE INDEX idx_orders_homestay_checkin ON orders (homestay_id, check_in_date);
    END IF;
END //

-- 2. 添加唯一索引
CREATE PROCEDURE create_unique_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'orders' 
                   AND INDEX_NAME = 'idx_orders_booking_unique') THEN
        CREATE UNIQUE INDEX idx_orders_booking_unique ON orders (homestay_id, check_in_date, check_out_date);
    END IF;
END //

-- 3. 添加状态索引
CREATE PROCEDURE create_status_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'orders' 
                   AND INDEX_NAME = 'idx_orders_status_check') THEN
        CREATE INDEX idx_orders_status_check ON orders (status, check_in_date, check_out_date);
    END IF;
END //

DELIMITER ;

-- 执行存储过程
CALL create_index_if_not_exists();
CALL create_unique_index_if_not_exists();
CALL create_status_index_if_not_exists();

-- 删除存储过程
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
DROP PROCEDURE IF EXISTS create_unique_index_if_not_exists;
DROP PROCEDURE IF EXISTS create_status_index_if_not_exists;
