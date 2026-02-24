-- 更新房源状态数据，删除已废弃的状态
-- V1.3 - 清理多审核员相关状态数据

-- 更新现有数据中的状态（如果有的话）
-- 将UNDER_REVIEW状态更新为PENDING
UPDATE homestays 
SET status = 'PENDING' 
WHERE status = 'UNDER_REVIEW';

-- 将RESUBMITTED状态更新为PENDING
UPDATE homestays 
SET status = 'PENDING' 
WHERE status = 'RESUBMITTED';

-- 更新审核日志中的状态引用
UPDATE homestay_audit_logs 
SET old_status = 'PENDING' 
WHERE old_status IN ('UNDER_REVIEW', 'RESUBMITTED');

UPDATE homestay_audit_logs 
SET new_status = 'PENDING' 
WHERE new_status IN ('UNDER_REVIEW', 'RESUBMITTED'); 