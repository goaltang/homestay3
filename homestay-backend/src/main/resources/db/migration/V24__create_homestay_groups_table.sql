-- 创建房源分组表
-- 用于房东对房源进行分组管理（如"海景房"、"别墅"、"公寓"等）

CREATE TABLE IF NOT EXISTS homestay_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    code VARCHAR(50) COMMENT '分组编码',
    description TEXT COMMENT '分组描述',
    icon VARCHAR(100) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色标识',
    owner_id BIGINT NOT NULL COMMENT '房东ID',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认分组',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_group_owner (owner_id),
    INDEX idx_group_code (code),
    CONSTRAINT fk_group_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源分组表';

-- 为房源表添加分组关联字段
ALTER TABLE homestays 
ADD COLUMN group_id BIGINT COMMENT '所属分组ID',
ADD INDEX idx_homestay_group (group_id),
ADD CONSTRAINT fk_homestay_group FOREIGN KEY (group_id) REFERENCES homestay_groups(id) ON DELETE SET NULL;
