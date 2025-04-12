-- 添加房东相关字段到用户表
ALTER TABLE users ADD COLUMN nickname VARCHAR(50);
ALTER TABLE users ADD COLUMN occupation VARCHAR(100);
ALTER TABLE users ADD COLUMN introduction TEXT;
ALTER TABLE users ADD COLUMN languages TEXT;
ALTER TABLE users ADD COLUMN host_since TIMESTAMP;
ALTER TABLE users ADD COLUMN host_rating DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN host_accommodations INTEGER;
ALTER TABLE users ADD COLUMN host_years INTEGER;
ALTER TABLE users ADD COLUMN host_response_rate VARCHAR(20);
ALTER TABLE users ADD COLUMN host_response_time VARCHAR(50);
ALTER TABLE users ADD COLUMN companions TEXT; 