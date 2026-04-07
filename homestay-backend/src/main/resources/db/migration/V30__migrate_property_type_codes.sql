-- V30__migrate_property_type_codes.sql
-- 将 homestays.type 从中文改为标准 code
-- 同时初始化 homestay_types 表（如果尚未初始化）

-- 1. 初始化 homestay_types 表（如果表存在且为空，才插入）
-- 注意：此表由 JPA ddl-auto=update 自动创建，这里只补充种子数据

-- 先检查是否已有数据
INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'ENTIRE', '整套公寓', NULL, '整套房源，独立私密空间', true, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'ENTIRE');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'PRIVATE', '独立房间', NULL, '独立房间，与房东共享房源', true, 2, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'PRIVATE');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'LOFT', '复式住宅', NULL, '复式/跃层结构，宽敞明亮', true, 3, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'LOFT');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'VILLA', '别墅', NULL, '独栋别墅，设施齐全', true, 4, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'VILLA');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'STUDIO', '开间/单间', NULL, '单间公寓，适合单人或双人', true, 5, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'STUDIO');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'TOWNHOUSE', '联排别墅', NULL, '联排别墅，共享社区', true, 6, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'TOWNHOUSE');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'COURTYARD', '四合院/院子', NULL, '传统四合院，老北京风格', true, 7, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'COURTYARD');

INSERT INTO homestay_types (code, name, icon, description, active, sort_order, created_at, updated_at)
SELECT 'HOTEL', '酒店公寓', NULL, '酒店式公寓，商务风格', true, 8, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM homestay_types WHERE code = 'HOTEL');

-- 2. 将 homestays.type 从中文迁移到 code
UPDATE homestays SET type = 'ENTIRE' WHERE type IN ('整套公寓', '整套房子');
UPDATE homestays SET type = 'PRIVATE' WHERE type IN ('独立房间', '单间');
UPDATE homestays SET type = 'LOFT' WHERE type IN ('复式住宅', 'Loft', 'LOFT');
UPDATE homestays SET type = 'VILLA' WHERE type IN ('别墅', '洋房');
UPDATE homestays SET type = 'COURTYARD' WHERE type IN ('四合院', '家庭旅馆', '院子');
UPDATE homestays SET type = 'HOTEL' WHERE type IN ('酒店公寓');
