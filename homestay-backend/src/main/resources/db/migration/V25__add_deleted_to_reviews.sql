-- 添加软删除字段到 reviews 表
ALTER TABLE reviews ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 为 deleted 字段添加索引以优化查询性能
CREATE INDEX idx_reviews_deleted ON reviews(deleted);

-- 可选：添加复合索引优化常见查询
CREATE INDEX idx_reviews_homestay_public_deleted ON reviews(homestay_id, is_public, deleted);
