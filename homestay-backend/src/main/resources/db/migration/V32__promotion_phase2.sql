-- ============================================================
-- 营销促销系统第二期数据库改造
-- 房东折扣活动、优惠券承担方、预算精细化
-- ============================================================

-- 1. 营销活动表增加房东相关字段和预算预警字段
ALTER TABLE promotion_campaign
    ADD COLUMN host_id BIGINT COMMENT '房东用户ID，平台活动为NULL',
    ADD COLUMN budget_alert_threshold DECIMAL(5, 2) DEFAULT 0.80 COMMENT '预算预警阈值，如0.8表示80%',
    ADD COLUMN budget_alert_triggered BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已触发预算预警';

-- 房东活动索引
CREATE INDEX idx_host_campaign ON promotion_campaign(host_id, status);

-- 2. 优惠券模板表增加承担方和房东字段
ALTER TABLE coupon_template
    ADD COLUMN subsidy_bearer VARCHAR(50) NOT NULL DEFAULT 'PLATFORM' COMMENT '承担方: PLATFORM(平台), HOST(房东), MIXED(混合)',
    ADD COLUMN host_id BIGINT COMMENT '房东用户ID，平台券为NULL';

-- 房东券模板索引
CREATE INDEX idx_host_coupon_template ON coupon_template(host_id, status);
