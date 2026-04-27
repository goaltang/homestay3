-- ============================================================
-- Flyway V35: 行为埋点 + 用户偏好画像表
-- 为 ES 搜索和个性化推荐提供数据基础
-- ============================================================

-- 1. 用户行为事件表
CREATE TABLE IF NOT EXISTS user_behavior_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID（未登录为NULL）',
    session_id VARCHAR(64) COMMENT '会话ID',
    event_type VARCHAR(20) NOT NULL COMMENT '事件类型: VIEW/SEARCH/CLICK/FAVORITE/BOOKING/SHARE',
    homestay_id BIGINT COMMENT '房源ID（如涉及）',
    keyword VARCHAR(200) COMMENT '搜索关键词',
    city_code VARCHAR(20) COMMENT '城市代码',
    type VARCHAR(50) COMMENT '房源类型',
    price DECIMAL(12, 2) COMMENT '房源价格快照',
    extra_json JSON COMMENT '扩展字段',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_event (user_id, event_type, created_at),
    INDEX idx_session_event (session_id, created_at),
    INDEX idx_homestay_event (homestay_id, event_type, created_at),
    INDEX idx_event_type_created (event_type, created_at),
    INDEX idx_city_event (city_code, event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为事件表';

-- 2. 用户偏好画像表
CREATE TABLE IF NOT EXISTS user_preference_profile (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID',
    preferred_city_json JSON COMMENT '偏好城市及权重',
    preferred_type_json JSON COMMENT '偏好房型及权重',
    preferred_amenity_json JSON COMMENT '偏好设施及权重',
    min_price DECIMAL(12, 2) COMMENT '历史浏览价格下限',
    max_price DECIMAL(12, 2) COMMENT '历史浏览价格上限',
    last_active_at DATETIME COMMENT '最后活跃时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_last_active (last_active_at),
    INDEX idx_price_range (min_price, max_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好画像表';
