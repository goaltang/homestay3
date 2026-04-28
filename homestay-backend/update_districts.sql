SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 删除旧的无 district 测试数据
DELETE FROM homestays WHERE id >= 100;

-- 导入新生成的测试数据（通过外部文件执行）
-- source seed_homestays.sql;

SET FOREIGN_KEY_CHECKS = 1;
