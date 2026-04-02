-- 创建评价图片表
CREATE TABLE review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);

-- 为 review_id 添加索引
CREATE INDEX idx_review_images_review_id ON review_images(review_id);

-- 为排序字段添加索引
CREATE INDEX idx_review_images_sort_order ON review_images(sort_order);
