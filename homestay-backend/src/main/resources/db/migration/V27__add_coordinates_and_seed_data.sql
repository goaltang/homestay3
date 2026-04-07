-- V27__add_coordinates_and_seed_data.sql
-- 合并迁移：添加经纬度字段 + 插入测试数据
-- 注意：此迁移仅用于开发环境，生产环境请删除

-- ============================================
-- 第一部分：添加经纬度字段（如果不存在）
-- ============================================
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homestays' AND COLUMN_NAME = 'latitude') = 0,
    'ALTER TABLE homestays ADD COLUMN latitude DECIMAL(10, 7) COMMENT "纬度"',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homestays' AND COLUMN_NAME = 'longitude') = 0,
    'ALTER TABLE homestays ADD COLUMN longitude DECIMAL(10, 7) COMMENT "经度"',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为经纬度添加索引以优化地图查询性能（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homestays' AND INDEX_NAME = 'idx_homestays_coordinates') = 0,
    'CREATE INDEX idx_homestays_coordinates ON homestays(latitude, longitude)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 第二部分：插入测试房源数据
-- ============================================

-- 先更新现有的5个房源的经纬度（如果latitude为NULL）
UPDATE homestays SET latitude = 30.2741, longitude = 120.1551 WHERE city_text = '杭州市' AND district_text = '西湖区' AND latitude IS NULL;
UPDATE homestays SET latitude = 31.2405, longitude = 121.4901 WHERE city_text = '上海市' AND district_text = '黄浦区' AND latitude IS NULL;
UPDATE homestays SET latitude = 26.8720, longitude = 100.2297 WHERE city_text = '丽江市' AND district_text = '古城区' AND latitude IS NULL;
UPDATE homestays SET latitude = 39.9349, longitude = 116.4022 WHERE city_text = '北京市' AND district_text = '东城区' AND latitude IS NULL;
UPDATE homestays SET latitude = 30.6716, longitude = 104.0658 WHERE city_text = '成都市' AND district_text = '青羊区' AND latitude IS NULL;

-- 插入额外的10个测试房源（使用 INSERT IGNORE 避免主键冲突）
INSERT IGNORE INTO homestays (title, type, price, status, max_guests, min_nights, max_nights,
    province_text, city_text, district_text, address_detail, province_code, city_code, district_code,
    description, cover_image, owner_id, featured, auto_confirm, check_in_time, check_out_time,
    cancel_policy_type, house_rules, latitude, longitude, created_at, updated_at)
VALUES
('鼓浪屿海边别墅', '整套房子', 888.00, 'ACTIVE', 10, 2, 14,
 '福建省', '厦门市', '思明区', '鼓浪屿晃岩路66号', '350200', '350200', '350203',
 '独栋海边别墅，带花园和露台。步行5分钟到海边，听浪声入睡。',
 'https://picsum.photos/seed/homestay6/800/600', 1, true, true, '14:00', '12:00',
 2, '请勿在室内吸烟', 24.4461, 118.0630, NOW(), NOW()),

('珠江新城现代公寓', '整套房子', 458.00, 'ACTIVE', 3, 1, 30,
 '广东省', '广州市', '天河区', '珠江东路128号', '440000', '440100', '440106',
 '广州CBD核心位置，地铁上盖。夜景超美，购物娱乐便利。',
 'https://picsum.photos/seed/homestay7/800/600', 1, false, true, '15:00', '11:00',
 1, NULL, 23.1195, 113.3213, NOW(), NOW()),

('园林旁古色古香宅院', '整套房子', 428.00, 'ACTIVE', 6, 1, 7,
 '江苏省', '苏州市', '姑苏区', '平江路108号', '320500', '320500', '320508',
 '紧邻拙政园，苏式园林风格。感受江南水乡的诗意生活。',
 'https://picsum.photos/seed/homestay8/800/600', 1, true, true, '14:00', '12:00',
 2, '请勿在室内大声喧哗', 31.3236, 120.6253, NOW(), NOW()),

('秦淮河畔夜景公寓', '整套房子', 358.00, 'ACTIVE', 4, 1, 10,
 '江苏省', '南京市', '秦淮区', '秦淮河畔小区', '320100', '320100', '320104',
 '窗外就是秦淮河夜景，步行可达夫子庙。房间温馨舒适。',
 'https://picsum.photos/seed/homestay9/800/600', 1, false, true, '14:00', '12:00',
 1, NULL, 32.0415, 118.7881, NOW(), NOW()),

('青岛金沙滩度假屋', '整套房子', 498.00, 'ACTIVE', 8, 3, 14,
 '山东省', '青岛市', '黄岛区', '金沙滩路288号', '370200', '370200', '370211',
 '距离金沙滩步行3分钟，适合家庭度假。厨房可做饭，提供海鲜加工服务。',
 'https://picsum.photos/seed/homestay10/800/600', 1, true, true, '15:00', '10:00',
 2, '禁止在室内吸烟', 35.8966, 120.0913, NOW(), NOW()),

('洪崖洞江景房', '整套房子', 378.00, 'ACTIVE', 4, 1, 7,
 '重庆市', '重庆市', '渝中区', '嘉滨路199号', '500000', '500100', '500103',
 '正对洪崖洞，夜景绝佳。楼下多条地铁线，交通四通八达。',
 'https://picsum.photos/seed/homestay11/800/600', 1, false, true, '14:00', '12:00',
 1, '山城爬坡较多，请注意安全', 29.5583, 106.5786, NOW(), NOW()),

('海棠湾独栋海景别墅', '整套房子', 1288.00, 'ACTIVE', 12, 3, 30,
 '海南省', '三亚市', '海棠区', '海棠湾度假区', '460000', '460200', '460203',
 '独栋海景别墅，带私人泳池。距离免税店5分钟车程。',
 'https://picsum.photos/seed/homestay12/800/600', 1, true, true, '15:00', '11:00',
 2, '请勿在泳池使用沐浴露等', 18.3958, 109.7457, NOW(), NOW()),

('大唐不夜城旁雅居', '整套房子', 298.00, 'ACTIVE', 5, 1, 7,
 '陕西省', '西安市', '雁塔区', '大唐不夜城小区', '610000', '610100', '610113',
 '紧邻大唐不夜城，步行可达大雁塔。房间古风装修，文化氛围浓厚。',
 'https://picsum.photos/seed/homestay13/800/600', 1, false, true, '14:00', '12:00',
 1, NULL, 34.2059, 108.9402, NOW(), NOW()),

('橘子洲头江景房', '整套房子', 328.00, 'ACTIVE', 4, 1, 7,
 '湖南省', '长沙市', '岳麓区', '橘子洲头小区', '430000', '430100', '430104',
 '俯瞰湘江，橘子洲头就在眼前。夜晚灯光秀美不胜收。',
 'https://picsum.photos/seed/homestay14/800/600', 1, false, true, '14:00', '12:00',
 1, NULL, 28.2283, 112.9388, NOW(), NOW()),

('东湖边温馨小屋', '整套房子', 268.00, 'ACTIVE', 4, 1, 14,
 '湖北省', '武汉市', '武昌区', '东湖路168号', '420000', '420100', '420106',
 '紧邻东湖绿道，空气清新。适合骑行、慢跑、赏湖景。',
 'https://picsum.photos/seed/homestay15/800/600', 1, false, true, '14:00', '12:00',
 1, NULL, 30.5489, 114.3671, NOW(), NOW());
