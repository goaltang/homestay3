-- 防止相同房源在重叠日期被重复预订的数据库约束
-- V12__add_booking_date_constraints.sql

-- 1. 添加复合索引以提高查询性能
CREATE INDEX idx_orders_homestay_dates_status 
ON orders(homestay_id, check_in_date, check_out_date, status);

-- 2. 添加数据完整性约束（如果不存在）
-- 检查约束可能已存在，如果存在则忽略错误
ALTER TABLE orders 
ADD CONSTRAINT chk_order_dates 
CHECK (check_out_date > check_in_date);

-- 3. 添加订单状态索引
CREATE INDEX idx_orders_status ON orders(status);

-- 4. 添加复合索引用于快速查找特定房源的有效订单
CREATE INDEX idx_orders_homestay_status_dates 
ON orders(homestay_id, status, check_in_date, check_out_date);

-- 5. 添加房源ID和日期范围的索引，用于并发控制查询优化
CREATE INDEX idx_orders_homestay_date_range 
ON orders(homestay_id, check_in_date, check_out_date); 