-- 创建日历占用表 - 第三道防线（最后底线）
-- 用于防止预订日期冲突，作为 Redis 锁和数据库查询之外的额外保护

CREATE TABLE IF NOT EXISTS homestay_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    homestay_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    order_id BIGINT,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    lock_expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_homestay_date (homestay_id, date),
    INDEX idx_homestay_date (homestay_id, date),
    INDEX idx_date_status (date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 注释说明表用途
ALTER TABLE homestay_availability COMMENT = '房源可用性日历表，用于记录每个房源每天的预订状态';
