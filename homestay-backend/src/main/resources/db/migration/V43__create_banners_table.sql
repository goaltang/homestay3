CREATE TABLE IF NOT EXISTS banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT 'Banner标题',
    subtitle VARCHAR(500) COMMENT '副标题/描述',
    image_url VARCHAR(500) COMMENT '背景图片URL（优先使用）',
    link_url VARCHAR(500) COMMENT '点击跳转链接',
    bg_gradient VARCHAR(200) COMMENT 'CSS渐变背景（无图片时使用）',
    sort_order INT DEFAULT 0 COMMENT '排序权重，越小越靠前',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页运营Banner表';

-- 插入默认Banner数据（幂等：先删后插）
DELETE FROM banners WHERE id IN (1, 2, 3);

INSERT INTO banners (id, title, subtitle, image_url, link_url, bg_gradient, sort_order, enabled) VALUES
(1, '发现独特民宿', '像当地人一样旅行', NULL, '/homestays', 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)', 1, true),
(2, '周末逃离城市', '精选周边度假房源', NULL, '/homestays?search=true', 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)', 2, true),
(3, '成为房东', '分享你的空间，获得收益', NULL, '/host/onboarding', 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)', 3, true);
