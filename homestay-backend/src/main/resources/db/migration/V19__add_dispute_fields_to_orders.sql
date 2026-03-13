-- 添加争议相关字段
ALTER TABLE orders ADD COLUMN dispute_reason TEXT;
ALTER TABLE orders ADD COLUMN dispute_raised_by BIGINT;
ALTER TABLE orders ADD COLUMN dispute_raised_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN dispute_resolved_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN dispute_resolution VARCHAR(20);
ALTER TABLE orders ADD COLUMN dispute_resolution_note VARCHAR(500);
