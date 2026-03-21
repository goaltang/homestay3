-- 争议记录表
CREATE TABLE IF NOT EXISTS dispute_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    dispute_reason TEXT COMMENT '争议原因',
    raised_by BIGINT COMMENT '发起争议的用户ID',
    raised_at DATETIME COMMENT '发起时间',
    resolution VARCHAR(20) COMMENT '仲裁结果：APPROVED/REJECTED',
    resolved_by BIGINT COMMENT '仲裁人ID',
    resolved_at DATETIME COMMENT '仲裁时间',
    resolution_note VARCHAR(500) COMMENT '仲裁备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_raised_by (raised_by),
    INDEX idx_resolved_by (resolved_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
