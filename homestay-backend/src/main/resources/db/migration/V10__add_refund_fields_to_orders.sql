-- 为订单表添加退款相关字段
-- V10__add_refund_fields_to_orders.sql

-- 添加退款类型字段
ALTER TABLE orders ADD COLUMN refund_type VARCHAR(50);

-- 添加退款原因字段  
ALTER TABLE orders ADD COLUMN refund_reason TEXT;

-- 添加退款金额字段
ALTER TABLE orders ADD COLUMN refund_amount DECIMAL(10,2);

-- 添加退款发起者ID字段
ALTER TABLE orders ADD COLUMN refund_initiated_by BIGINT;

-- 添加退款发起时间字段
ALTER TABLE orders ADD COLUMN refund_initiated_at TIMESTAMP;

-- 添加退款处理者ID字段
ALTER TABLE orders ADD COLUMN refund_processed_by BIGINT;

-- 添加退款处理时间字段
ALTER TABLE orders ADD COLUMN refund_processed_at TIMESTAMP;

-- 添加退款交易号字段
ALTER TABLE orders ADD COLUMN refund_transaction_id VARCHAR(100);

-- 为退款相关字段添加索引
CREATE INDEX idx_orders_refund_type ON orders(refund_type);
CREATE INDEX idx_orders_refund_initiated_by ON orders(refund_initiated_by);
CREATE INDEX idx_orders_refund_processed_by ON orders(refund_processed_by); 