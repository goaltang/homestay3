-- V14: 添加支付记录表
-- 用于记录支付网关的交易记录

CREATE TABLE payment_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    payment_method VARCHAR(20) NOT NULL COMMENT '支付方式 (ALIPAY, WECHAT)',
    out_trade_no VARCHAR(64) NOT NULL UNIQUE COMMENT '商户订单号',
    transaction_id VARCHAR(64) COMMENT '支付宝交易号',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态 (PENDING, SUCCESS, FAILED)',
    request_params TEXT COMMENT '请求参数JSON',
    response_params TEXT COMMENT '响应参数JSON',
    notify_params TEXT COMMENT '回调参数JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_order_id (order_id),
    INDEX idx_out_trade_no (out_trade_no),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_payment_method (payment_method),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) COMMENT = '支付记录表';

-- 检查并添加支付状态字段（如果不存在）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'payment_status';

SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20) DEFAULT ''UNPAID'' COMMENT ''支付状态 (UNPAID, PAID, PAYMENT_FAILED, REFUNDED, PARTIALLY_REFUNDED)'' AFTER status',
  'SELECT ''payment_status column already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加索引（如果不存在）
SET @idx_exists = 0;
SELECT COUNT(*) INTO @idx_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'idx_payment_status';

SET @sql = IF(@idx_exists = 0, 
  'ALTER TABLE orders ADD INDEX idx_payment_status (payment_status)',
  'SELECT ''idx_payment_status index already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 