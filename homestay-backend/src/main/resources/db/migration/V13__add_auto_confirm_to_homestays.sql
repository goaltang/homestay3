-- V13: 为民宿表添加自动确认字段，支持直接付费模式
-- 作者: AI Assistant
-- 日期: 2024-12-19

-- 1. 添加自动确认字段
ALTER TABLE homestays ADD COLUMN auto_confirm BOOLEAN DEFAULT FALSE COMMENT '是否自动确认订单（直接付费模式）';

-- 2. 根据价格设置初始值：500元以下的房源默认开启自动确认
UPDATE homestays 
SET auto_confirm = TRUE 
WHERE price < 500 AND status = 'ACTIVE';

-- 3. 为新字段添加索引以提高查询性能
CREATE INDEX idx_homestays_auto_confirm ON homestays(auto_confirm);

-- 4. 添加注释说明
ALTER TABLE homestays MODIFY COLUMN auto_confirm BOOLEAN DEFAULT FALSE COMMENT '自动确认订单：true=直接付费，false=房东确认制'; 