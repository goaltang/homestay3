-- 初始化管理员账号 (密码: 123456)
INSERT INTO admin (username, password, created_at, updated_at)
VALUES ('admin', '$2a$10$rTm8LHhxDeDqXEHFxAEfZOXu7yJrKzJ3k8DMZ9x/aV0YH0Ln.qKxC', NOW(), NOW())
ON CONFLICT (username) DO NOTHING; 