-- ============================================================
-- 营销促销系统第三期：阶梯满减、订单返券、券互斥组
-- ============================================================

-- 1. 活动规则表增加阶梯配置（支持多档满减）
ALTER TABLE promotion_rule
    ADD COLUMN tier_config_json TEXT COMMENT '阶梯配置JSON，如 [{"threshold":300,"discount":30},{"threshold":500,"discount":60}]';

-- 2. 优惠券模板表增加返券触发条件和互斥组
ALTER TABLE coupon_template
    ADD COLUMN auto_issue_trigger VARCHAR(50) DEFAULT 'NONE' COMMENT '自动发放触发条件: NONE/ORDER_COMPLETED/FIRST_ORDER/REGISTER',
    ADD COLUMN stack_group VARCHAR(50) DEFAULT 'DEFAULT' COMMENT '互斥组ID，同组券不可叠加';

-- 3. 互斥组索引
CREATE INDEX idx_stack_group ON coupon_template(stack_group, status);
CREATE INDEX idx_auto_issue_trigger ON coupon_template(auto_issue_trigger, status);
