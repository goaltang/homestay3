-- 为 user_coupon 表添加领取快照字段，防止后台修改模板后追溯影响已发放券
ALTER TABLE user_coupon
    ADD COLUMN snapshot_face_value DECIMAL(12, 2) NULL AFTER expire_at,
    ADD COLUMN snapshot_discount_rate DECIMAL(3, 2) NULL AFTER snapshot_face_value,
    ADD COLUMN snapshot_threshold_amount DECIMAL(12, 2) NULL AFTER snapshot_discount_rate,
    ADD COLUMN snapshot_max_discount DECIMAL(12, 2) NULL AFTER snapshot_threshold_amount,
    ADD COLUMN snapshot_scope_type VARCHAR(50) NULL AFTER snapshot_max_discount,
    ADD COLUMN snapshot_scope_value_json TEXT NULL AFTER snapshot_scope_type,
    ADD COLUMN snapshot_subsidy_bearer VARCHAR(50) NULL AFTER snapshot_scope_value_json;
