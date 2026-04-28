-- ============================================================
-- 营销促销系统第四期：批量发券、分享裂变、转化漏斗
-- ============================================================

-- 1. 批量发券任务表
CREATE TABLE IF NOT EXISTS coupon_batch_issue_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '优惠券模板ID',
    name VARCHAR(200) NOT NULL COMMENT '任务名称',
    filter_type VARCHAR(50) NOT NULL COMMENT '筛选类型: ALL/AT_RISK/HIGH_VALUE/NEW_USER/NO_ORDER_30D',
    filter_params_json TEXT COMMENT '筛选参数JSON',
    total_count INT NOT NULL DEFAULT 0 COMMENT '目标用户总数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功发放数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    error_msg TEXT COMMENT '错误信息',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_created (status, created_at),
    INDEX idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量发券任务表';

-- 2. 批量发券任务明细表
CREATE TABLE IF NOT EXISTS coupon_batch_issue_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SUCCESS/FAILED',
    error_msg VARCHAR(500) COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES coupon_batch_issue_task(id) ON DELETE CASCADE,
    INDEX idx_task_user (task_id, user_id),
    INDEX idx_task_status (task_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量发券任务明细表';

-- 3. 邀请裂变记录表
CREATE TABLE IF NOT EXISTS referral_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    referral_code VARCHAR(32) NOT NULL UNIQUE COMMENT '邀请码',
    inviter_id BIGINT NOT NULL COMMENT '邀请人用户ID',
    template_id_for_invitee BIGINT COMMENT '被邀请人奖励券模板ID',
    template_id_for_inviter BIGINT COMMENT '邀请人奖励券模板ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/USED/EXPIRED',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已被使用次数',
    max_uses INT NOT NULL DEFAULT 1 COMMENT '最大可使用次数',
    expire_at DATETIME COMMENT '邀请码过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_code (referral_code),
    INDEX idx_inviter (inviter_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请裂变记录表';

-- 4. 优惠券分析事件表（转化漏斗）
CREATE TABLE IF NOT EXISTS coupon_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT COMMENT '优惠券模板ID（活动相关则为NULL）',
    campaign_id BIGINT COMMENT '活动ID（券相关则为NULL）',
    channel VARCHAR(50) COMMENT '渠道: HOME_BANNER/HOMESTAY_DETAIL/ORDER_SUCCESS/SHARE_LINK/SMS',
    event_type VARCHAR(20) NOT NULL COMMENT '事件类型: IMPRESSION/CLICK/CLAIM/LOCK/USE/EXPIRE',
    user_id BIGINT COMMENT '用户ID',
    homestay_id BIGINT COMMENT '房源ID（如涉及）',
    order_id BIGINT COMMENT '订单ID（如涉及）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template_event (template_id, event_type, created_at),
    INDEX idx_campaign_event (campaign_id, event_type, created_at),
    INDEX idx_channel_created (channel, created_at),
    INDEX idx_user_event (user_id, event_type, created_at),
    INDEX idx_event_created (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券分析事件表';
