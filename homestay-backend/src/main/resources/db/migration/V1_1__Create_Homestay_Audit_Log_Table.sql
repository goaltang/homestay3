-- 创建房源审核记录表
-- 版本: V1.1
-- 描述: 为房源审核机制添加审核记录表，用于追踪所有审核操作的历史

-- 创建审核记录表
CREATE TABLE homestay_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    homestay_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    review_reason TEXT,
    review_notes TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_audit_homestay 
        FOREIGN KEY (homestay_id) 
        REFERENCES homestays(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_audit_reviewer 
        FOREIGN KEY (reviewer_id) 
        REFERENCES users(id) 
        ON DELETE RESTRICT,
    
    -- 索引优化
    INDEX idx_homestay_audit_homestay_id (homestay_id),
    INDEX idx_homestay_audit_reviewer_id (reviewer_id),
    INDEX idx_homestay_audit_created_at (created_at),
    INDEX idx_homestay_audit_status (new_status),
    INDEX idx_homestay_audit_action_type (action_type)
);

-- 更新现有房源表，将字符串状态转换为枚举兼容格式
-- 注意：这个脚本假设现有状态已经是标准格式（ACTIVE, INACTIVE, PENDING, REJECTED）

-- 添加状态约束，确保只能使用预定义的状态值
ALTER TABLE homestays 
ADD CONSTRAINT chk_homestay_status 
CHECK (status IN ('DRAFT', 'PENDING', 'UNDER_REVIEW', 'ACTIVE', 'INACTIVE', 'REJECTED', 'RESUBMITTED', 'SUSPENDED'));

-- 为现有数据创建初始审核记录（可选）
-- 这将为所有现有的房源创建一条"系统初始化"的审核记录
INSERT INTO homestay_audit_logs (homestay_id, reviewer_id, old_status, new_status, action_type, review_reason, review_notes, created_at)
SELECT 
    h.id as homestay_id,
    1 as reviewer_id, -- 假设ID为1的用户是系统管理员，需要根据实际情况调整
    NULL as old_status,
    h.status as new_status,
    'SUBMIT' as action_type,
    '系统数据迁移' as review_reason,
    '从现有数据自动生成的审核记录' as review_notes,
    h.created_at as created_at
FROM homestays h
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1); -- 确保管理员用户存在

-- 创建审核统计视图（可选）
CREATE VIEW v_homestay_audit_stats AS
SELECT 
    DATE(created_at) as audit_date,
    action_type,
    new_status,
    COUNT(*) as operation_count,
    COUNT(DISTINCT homestay_id) as unique_homestays,
    COUNT(DISTINCT reviewer_id) as unique_reviewers
FROM homestay_audit_logs
GROUP BY DATE(created_at), action_type, new_status
ORDER BY audit_date DESC, action_type;

-- 创建审核员工作量统计视图
CREATE VIEW v_reviewer_workload AS
SELECT 
    u.id as reviewer_id,
    u.username as reviewer_name,
    DATE(al.created_at) as work_date,
    COUNT(*) as total_reviews,
    COUNT(CASE WHEN al.action_type = 'APPROVE' THEN 1 END) as approvals,
    COUNT(CASE WHEN al.action_type = 'REJECT' THEN 1 END) as rejections,
    ROUND(
        COUNT(CASE WHEN al.action_type = 'APPROVE' THEN 1 END) * 100.0 / 
        COUNT(CASE WHEN al.action_type IN ('APPROVE', 'REJECT') THEN 1 END), 2
    ) as approval_rate
FROM homestay_audit_logs al
JOIN users u ON al.reviewer_id = u.id
WHERE al.action_type IN ('APPROVE', 'REJECT')
GROUP BY u.id, u.username, DATE(al.created_at)
ORDER BY work_date DESC, total_reviews DESC; 