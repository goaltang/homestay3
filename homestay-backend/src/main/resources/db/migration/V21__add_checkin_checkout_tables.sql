-- 入住记录表
CREATE TABLE IF NOT EXISTS check_in_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    check_in_method VARCHAR(30),
    checked_in_at DATETIME,
    check_in_operator_id BIGINT,
    check_in_operator_type VARCHAR(20),
    check_in_code VARCHAR(32),
    door_password VARCHAR(32),
    lockbox_code VARCHAR(32),
    location_description VARCHAR(200),
    valid_from DATETIME,
    valid_until DATETIME,
    remark TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_check_in_order_id (order_id),
    INDEX idx_check_in_code (check_in_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 退房记录表
CREATE TABLE IF NOT EXISTS check_out_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    check_out_method VARCHAR(30),
    checked_out_at DATETIME,
    check_out_operator_id BIGINT,
    check_out_operator_type VARCHAR(20),
    actual_nights INT,
    deposit_amount DECIMAL(10,2),
    deposit_status VARCHAR(20),
    extra_charges DECIMAL(10,2),
    extra_charges_description TEXT,
    settlement_amount DECIMAL(10,2),
    remark TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_check_out_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 订单表新增入住相关字段
ALTER TABLE orders ADD COLUMN checked_in_at DATETIME AFTER refund_rejection_reason;
ALTER TABLE orders ADD COLUMN checked_out_at DATETIME AFTER checked_in_at;
ALTER TABLE orders ADD COLUMN check_in_code VARCHAR(32) AFTER checked_out_at;
ALTER TABLE orders ADD COLUMN door_password VARCHAR(32) AFTER check_in_code;
ALTER TABLE orders ADD COLUMN auto_checkin_time VARCHAR(5) AFTER door_password;
ALTER TABLE orders ADD COLUMN deposit_amount DECIMAL(10,2) AFTER auto_checkin_time;

-- 系统配置新增入住/退房时间配置
INSERT INTO system_config (config_key, config_value, config_name, category) VALUES
('AUTO_CHECKIN_TIME', '18:00', '自动入住时间', 'checkin'),
('AUTO_CHECKOUT_TIME', '12:00', '自动退房时间', 'checkout')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
