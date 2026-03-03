-- 退款记录表
-- V15__create_refund_records_table.sql

CREATE TABLE refund_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    out_trade_no VARCHAR(64),
    trade_no VARCHAR(64),
    refund_amount DECIMAL(10,2) NOT NULL,
    refund_reason TEXT,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    refund_type VARCHAR(50),
    out_request_no VARCHAR(64),
    refund_trade_no VARCHAR(64),
    request_params TEXT,
    response_params TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    INDEX idx_refund_records_order_id (order_id),
    INDEX idx_refund_records_status (refund_status)
);
