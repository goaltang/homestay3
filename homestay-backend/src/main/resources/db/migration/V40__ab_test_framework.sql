-- ============================================================
-- AB测试框架
-- ============================================================

CREATE TABLE IF NOT EXISTS ab_experiment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '实验名称',
    description TEXT COMMENT '实验描述',
    experiment_type VARCHAR(30) NOT NULL COMMENT '实验类型: CAMPAIGN/COUPON_TEMPLATE',
    target_id BIGINT NOT NULL COMMENT '目标对象ID（活动ID或券模板ID）',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/RUNNING/PAUSED/ENDED',
    start_at DATETIME COMMENT '开始时间',
    end_at DATETIME COMMENT '结束时间',
    traffic_percent INT NOT NULL DEFAULT 100 COMMENT '参与实验流量百分比(0-100)',
    primary_metric VARCHAR(50) NOT NULL DEFAULT 'CONVERSION' COMMENT '主要指标: CONVERSION/ORDER_RATE/GMV',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_target_type (target_id, experiment_type),
    INDEX idx_status (status, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AB实验表';

CREATE TABLE IF NOT EXISTS ab_variant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL COMMENT '实验ID',
    variant_key VARCHAR(30) NOT NULL COMMENT '组标识: control/variant_a/variant_b',
    name VARCHAR(100) NOT NULL COMMENT '组名称',
    traffic_ratio INT NOT NULL DEFAULT 50 COMMENT '流量占比(0-100)，所有组之和应为100',
    config_json TEXT COMMENT '实验配置JSON（如不同折扣力度、不同文案）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (experiment_id) REFERENCES ab_experiment(id) ON DELETE CASCADE,
    UNIQUE KEY uk_experiment_variant (experiment_id, variant_key),
    INDEX idx_experiment (experiment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AB实验组表';

CREATE TABLE IF NOT EXISTS ab_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL COMMENT '实验ID',
    variant_id BIGINT NOT NULL COMMENT '实验组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_experiment_user (experiment_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AB实验用户分流记录表';

CREATE TABLE IF NOT EXISTS ab_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL COMMENT '实验ID',
    variant_id BIGINT NOT NULL COMMENT '实验组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    event_type VARCHAR(30) NOT NULL COMMENT '事件类型: IMPRESSION/CLICK/CONVERT/ORDER',
    event_value DECIMAL(12,2) COMMENT '事件数值（如订单金额）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_experiment_variant_event (experiment_id, variant_id, event_type, created_at),
    INDEX idx_user_event (user_id, event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AB实验事件表';
