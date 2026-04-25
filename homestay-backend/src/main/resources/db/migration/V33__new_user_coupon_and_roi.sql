-- ============================================================
-- Flyway V33: 第三期 — 新人券 + ROI 分析增强
-- ============================================================

-- 1. 优惠券模板增加"新人券"标记
ALTER TABLE coupon_template
    ADD COLUMN is_new_user_coupon TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为新人专属券：1=是，0=否'
    AFTER host_id;

-- 2. promotion_usage 增加复合索引，加速 ROI 聚合查询
ALTER TABLE promotion_usage
    ADD INDEX idx_bearer_status (bearer, status),
    ADD INDEX idx_created_at (created_at),
    ADD INDEX idx_campaign_bearer (campaign_id, bearer);

-- 3. order_price_snapshot 增加索引，加速订单金额关联查询
ALTER TABLE order_price_snapshot
    ADD INDEX idx_payable_amount (order_id, payable_amount);
