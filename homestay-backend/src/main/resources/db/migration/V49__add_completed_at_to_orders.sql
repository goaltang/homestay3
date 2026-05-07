-- 为 orders 表添加 completed_at 字段，用于记录订单完成时间
ALTER TABLE orders ADD COLUMN completed_at DATETIME NULL COMMENT '订单完成时间（状态变为 COMPLETED 时设置）';

-- 为已有 COMPLETED 状态的订单回填 completed_at（使用 updated_at 作为近似值）
UPDATE orders SET completed_at = updated_at WHERE status = 'COMPLETED' AND completed_at IS NULL;
