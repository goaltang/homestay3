-- ============================================================
-- 营销促销系统数据库表结构
-- 包含：活动、活动规则、优惠券模板、用户优惠券、订单价格快照、优惠使用流水
-- ============================================================

-- 1. 营销活动表
CREATE TABLE IF NOT EXISTS promotion_campaign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '活动名称',
    campaign_type VARCHAR(50) NOT NULL COMMENT '活动类型: FLASH_SALE(限时折扣), FULL_REDUCTION(满减), HOMESTAY_DISCOUNT(房源折扣)',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/ACTIVE/PAUSED/ENDED',
    start_at DATETIME COMMENT '开始时间',
    end_at DATETIME COMMENT '结束时间',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大越优先',
    stackable BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否可与其他活动叠加',
    budget_total DECIMAL(12, 2) COMMENT '总预算，null表示无限制',
    budget_used DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '已使用预算',
    subsidy_bearer VARCHAR(50) NOT NULL DEFAULT 'PLATFORM' COMMENT '承担方: PLATFORM(平台), HOST(房东), MIXED(混合)',
    created_by VARCHAR(100) COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_time (status, start_at, end_at),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动表';

-- 2. 活动规则表
CREATE TABLE IF NOT EXISTS promotion_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL COMMENT '活动ID',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: AMOUNT_OFF(固定金额减免), PERCENT_OFF(百分比折扣), FULL_REDUCTION(满减), PER_NIGHT_OFF(每晚减免)',
    threshold_amount DECIMAL(12, 2) COMMENT '门槛金额（满减/满折用）',
    discount_amount DECIMAL(12, 2) COMMENT '固定优惠金额',
    discount_rate DECIMAL(3, 2) COMMENT '折扣率，如0.9表示9折',
    max_discount DECIMAL(12, 2) COMMENT '最大优惠金额',
    min_nights INT COMMENT '最小入住晚数',
    max_nights INT COMMENT '最大入住晚数',
    scope_type VARCHAR(50) NOT NULL DEFAULT 'ALL' COMMENT '范围类型: ALL, CITY, HOMESTAY, GROUP, HOST, TYPE',
    scope_value_json TEXT COMMENT '范围值JSON',
    first_order_only BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否仅限首单',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (campaign_id) REFERENCES promotion_campaign(id) ON DELETE CASCADE,
    INDEX idx_campaign (campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动规则表';

-- 3. 优惠券模板表
CREATE TABLE IF NOT EXISTS coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '券名称',
    coupon_type VARCHAR(50) NOT NULL COMMENT '券类型: CASH(现金券), DISCOUNT(折扣券), FULL_REDUCTION(满减券)',
    face_value DECIMAL(12, 2) COMMENT '面值（现金券用）',
    discount_rate DECIMAL(3, 2) COMMENT '折扣率（折扣券用）',
    threshold_amount DECIMAL(12, 2) COMMENT '使用门槛金额',
    max_discount DECIMAL(12, 2) COMMENT '最大优惠金额',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    issued_count INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每用户限领数量',
    valid_type VARCHAR(50) NOT NULL COMMENT '有效期类型: FIXED_TIME(固定时间), AFTER_CLAIM_DAYS(领取后N天)',
    valid_days INT COMMENT '领取后有效天数',
    valid_start_at DATETIME COMMENT '固定有效期开始',
    valid_end_at DATETIME COMMENT '固定有效期结束',
    scope_type VARCHAR(50) NOT NULL DEFAULT 'ALL' COMMENT '适用范围类型',
    scope_value_json TEXT COMMENT '适用范围值JSON',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/ACTIVE/PAUSED/EXPIRED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_time (status, valid_start_at, valid_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 4. 用户优惠券表
CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '券模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_code VARCHAR(64) NOT NULL UNIQUE COMMENT '券码',
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE/LOCKED/USED/EXPIRED',
    locked_order_id BIGINT COMMENT '锁定订单ID',
    locked_at DATETIME COMMENT '锁定时间',
    used_order_id BIGINT COMMENT '使用订单ID',
    used_at DATETIME COMMENT '使用时间',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES coupon_template(id),
    INDEX idx_user_status (user_id, status, expire_at),
    INDEX idx_locked_order (locked_order_id),
    INDEX idx_used_order (used_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 5. 订单价格快照表
CREATE TABLE IF NOT EXISTS order_price_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
    quote_token VARCHAR(128) COMMENT '报价令牌',
    room_original_amount DECIMAL(12, 2) NOT NULL COMMENT '原始房费合计',
    daily_price_json TEXT NOT NULL COMMENT '每日价格明细JSON',
    activity_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '活动优惠',
    full_reduction_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '满减优惠',
    coupon_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠券优惠',
    platform_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '平台承担优惠',
    host_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '房东承担优惠',
    cleaning_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '清洁费',
    service_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '服务费',
    payable_amount DECIMAL(12, 2) NOT NULL COMMENT '用户实付金额',
    host_receivable_amount DECIMAL(12, 2) NOT NULL COMMENT '房东应收金额',
    pricing_detail_json TEXT COMMENT '完整定价明细JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单价格快照表';

-- 6. 优惠使用流水表
CREATE TABLE IF NOT EXISTS promotion_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    campaign_id BIGINT COMMENT '活动ID',
    coupon_id BIGINT COMMENT '用户券ID',
    discount_amount DECIMAL(12, 2) NOT NULL COMMENT '优惠金额',
    bearer VARCHAR(50) NOT NULL COMMENT '承担方: PLATFORM / HOST',
    status VARCHAR(50) NOT NULL DEFAULT 'LOCKED' COMMENT '状态: LOCKED/USED/RELEASED/REFUNDED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order (order_id),
    INDEX idx_user_campaign (user_id, campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠使用流水表';
