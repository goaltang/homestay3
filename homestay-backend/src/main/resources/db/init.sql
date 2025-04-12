-- 创建管理员表
CREATE TABLE IF NOT EXISTS admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 插入默认管理员账号 (密码: 123456)
INSERT INTO admin (username, password, created_at, updated_at)
VALUES ('admin', '$2a$10$rTm8LHhxDeDqXEHFxAEfZOXu7yJrKzJ3k8DMZ9x/aV0YH0Ln.qKxC', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW(); 