-- ============================================================
-- 定价引擎数据库表结构
-- 包含：节假日日历、价格规则
-- ============================================================

-- 1. 节假日日历表
CREATE TABLE IF NOT EXISTS holiday_calendar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50),
    region_code VARCHAR(20) NOT NULL DEFAULT 'CN',
    is_holiday BOOLEAN NOT NULL DEFAULT TRUE,
    is_makeup_workday BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_region (date, region_code),
    INDEX idx_date (date),
    INDEX idx_region_date (region_code, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节假日日历表';

-- 2. 价格规则表
CREATE TABLE IF NOT EXISTS pricing_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '规则名称',
    scope_type VARCHAR(50) NOT NULL COMMENT '作用域: GLOBAL/CITY/HOST/GROUP/HOMESTAY/TYPE',
    scope_value_json TEXT COMMENT '作用域值JSON',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: WEEKEND/HOLIDAY/DATE_RANGE/EARLY_BIRD/LONG_STAY',
    adjustment_type VARCHAR(50) NOT NULL COMMENT '调价方式: MULTIPLY/DISCOUNT_RATE/AMOUNT_OFF/FIXED_PRICE',
    adjustment_value DECIMAL(12, 4) NOT NULL COMMENT '调价数值',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越小越优先',
    stackable BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否可叠加',
    start_date DATE COMMENT '规则生效开始日期',
    end_date DATE COMMENT '规则生效结束日期',
    min_nights INT COMMENT '最少入住夜数',
    max_nights INT COMMENT '最多入住夜数',
    min_advance_days INT COMMENT '最少提前预订天数',
    max_advance_days INT COMMENT '最多提前预订天数',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scope (scope_type, enabled),
    INDEX idx_rule_type (rule_type, enabled),
    INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格规则表';

-- 3. 插入 2026 年中国节假日数据（替代硬编码）
INSERT INTO holiday_calendar (date, name, type, region_code, is_holiday, is_makeup_workday) VALUES
-- 元旦
('2026-01-01', '元旦', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
-- 春节
('2026-02-17', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-02-18', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-02-19', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-02-20', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-02-21', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-02-22', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-02-23', '春节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
-- 清明节
('2026-04-04', '清明节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-04-05', '清明节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-04-06', '清明节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
-- 劳动节
('2026-05-01', '劳动节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-05-02', '劳动节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-05-03', '劳动节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
-- 端午节
('2026-05-31', '端午节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-06-01', '端午节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-06-02', '端午节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
-- 国庆节+中秋节
('2026-10-01', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-10-02', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-10-03', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-10-04', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-10-05', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-10-06', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE),
('2026-10-07', '国庆节', 'PUBLIC_HOLIDAY', 'CN', TRUE, FALSE);

-- 4. 插入默认价格规则（全局）
-- 周末浮动
INSERT INTO pricing_rule (name, scope_type, scope_value_json, rule_type, adjustment_type, adjustment_value, priority, stackable, enabled)
VALUES ('全局周末浮动', 'GLOBAL', '{}', 'WEEKEND', 'MULTIPLY', 1.20, 10, TRUE, TRUE);

-- 节假日浮动
INSERT INTO pricing_rule (name, scope_type, scope_value_json, rule_type, adjustment_type, adjustment_value, priority, stackable, enabled)
VALUES ('全局节假日浮动', 'GLOBAL', '{}', 'HOLIDAY', 'MULTIPLY', 1.50, 20, TRUE, TRUE);

-- 早鸟优惠
INSERT INTO pricing_rule (name, scope_type, scope_value_json, rule_type, adjustment_type, adjustment_value, priority, stackable, min_advance_days, enabled)
VALUES ('早鸟30天9折', 'GLOBAL', '{}', 'EARLY_BIRD', 'DISCOUNT_RATE', 0.90, 30, TRUE, 30, TRUE);

-- 连住优惠
INSERT INTO pricing_rule (name, scope_type, scope_value_json, rule_type, adjustment_type, adjustment_value, priority, stackable, min_nights, enabled)
VALUES ('连住7晚85折', 'GLOBAL', '{}', 'LONG_STAY', 'DISCOUNT_RATE', 0.85, 40, TRUE, 7, TRUE);
