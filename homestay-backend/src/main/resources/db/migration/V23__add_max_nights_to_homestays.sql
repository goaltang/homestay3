-- 为 homestays 表添加最大入住晚数字段
-- null 表示不限制最大晚数

ALTER TABLE homestays ADD COLUMN max_nights INT DEFAULT NULL COMMENT '最大入住晚数，null表示不限制';

-- 为已有数据设置默认值：30晚（一个月）作为合理上限
UPDATE homestays SET max_nights = 30 WHERE max_nights IS NULL AND min_nights IS NOT NULL;

-- 为 min_nights > 1 的房源自动设置合理的 max_nights（最小值的 10 倍，上限 90 晚）
UPDATE homestays
SET max_nights = LEAST(min_nights * 10, 90)
WHERE max_nights IS NULL AND min_nights > 1;

-- 添加注释说明
ALTER TABLE homestays MODIFY COLUMN max_nights INT DEFAULT NULL COMMENT '最大入住晚数，null=不限制';

-- 可选：为 max_nights 添加索引（如果经常按此字段筛选）
-- CREATE INDEX idx_homestays_max_nights ON homestays(max_nights);
