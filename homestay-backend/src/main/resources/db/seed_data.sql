-- 1. 清理表数据（保留结构）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE homestay_amenity;
TRUNCATE TABLE homestay_images;
TRUNCATE TABLE homestays;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. 插入用户
-- 密码均为: 123456
-- 正确的 bcrypt 哈希 (通过注册功能验证)
INSERT INTO users (id, username, email, password, role, enabled, created_at, updated_at, verification_status, real_name)
VALUES 
(1, 'host1', 'host1@test.com', '$2a$10$oMeaCXPR2aItuzgcxsCEhOE1GWjakAlO87xZyyTjmfzFaTrZpDsD6', 'ROLE_HOST', 1, NOW(), NOW(), 'VERIFIED', '房东一号'),
(2, 'host2', 'host2@test.com', '$2a$10$oMeaCXPR2aItuzgcxsCEhOE1GWjakAlO87xZyyTjmfzFaTrZpDsD6', 'ROLE_HOST', 1, NOW(), NOW(), 'VERIFIED', '房东二号'),
(3, 'user1', 'user1@test.com', '$2a$10$oMeaCXPR2aItuzgcxsCEhOE1GWjakAlO87xZyyTjmfzFaTrZpDsD6', 'ROLE_USER', 1, NOW(), NOW(), 'VERIFIED', '普通用户一号');

-- 3. 插入房源 (增加了 cover_image 字段，这对前端显示至关重要)
INSERT INTO homestays (id, title, type, price, status, max_guests, min_nights, province_code, city_code, district_code, address_detail, description, owner_id, auto_confirm, cover_image, featured, created_at, updated_at)
VALUES
(1, '杭州西湖·断桥残雪湖景房', '整套公寓', 688.00, 'ACTIVE', 2, 1, '330000', '330100', '330106', '西湖区北山街1号', '推窗即见西湖，步行可达断桥', 1, 1, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800', 1, NOW(), NOW()),
(2, '成都太古里·繁华里Loft', '复式住宅', 458.00, 'ACTIVE', 4, 1, '510000', '510100', '510104', '锦江区中纱帽街', '现代简约风格，俯瞰太古里夜景', 2, 1, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800', 1, NOW(), NOW()),
(3, '大理古城·苍山下小院', '家庭旅馆', 299.00, 'ACTIVE', 3, 2, '530000', '532900', '532901', '大理市古城南门', '阳光充足的小院，可以看苍山雪', 1, 1, 'https://images.unsplash.com/photo-1505691938895-1758d7dde511?w=800', 0, NOW(), NOW()),
(4, '上海武康路·老洋房露台房', '洋房', 888.00, 'ACTIVE', 2, 1, '310000', '310100', '310104', '徐汇区武康路100号', '体验旧上海的风情，带超大私家露台', 1, 1, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800', 1, NOW(), NOW()),
(5, '北京三里屯·潮流艺术公寓', '整套公寓', 750.00, 'ACTIVE', 2, 1, '110000', '110100', '110105', '朝阳区工体北路', '波普艺术装修，极速Wi-Fi', 2, 1, 'https://images.unsplash.com/photo-1536376074432-cd42da8544af?w=800', 0, NOW(), NOW());

-- 4. 插入房源图片关联数据 (供详情页使用)
INSERT INTO homestay_images (homestay_id, image_url)
VALUES
(1, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800'),
(2, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800'),
(3, 'https://images.unsplash.com/photo-1505691938895-1758d7dde511?w=800'),
(4, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800'),
(5, 'https://images.unsplash.com/photo-1536376074432-cd42da8544af?w=800');
