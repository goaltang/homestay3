-- 订单表基线脚本
-- 创建 orders 表的基础结构（仅包含 V10 之前的核心字段）
-- 退款、争议、入住、幂等键、版本号等字段由后续 Flyway 脚本（V10, V14, V17, V19, V21, V41 等）逐步添加
-- 
-- 部署说明：
-- 1. 新环境：Flyway 会按 V1 -> V1.1 -> V1.2 -> ... -> V43 的顺序执行，本脚本正常创建基线表
-- 2. 已有环境（已应用到 V43+）：本脚本版本号 1.2 低于当前最大版本，Flyway 不会重复执行
-- 3. 建议后续将 spring.jpa.hibernate.ddl-auto 从 update 逐步迁移到 validate，统一由 Flyway 管理 schema

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_number VARCHAR(255) NOT NULL UNIQUE COMMENT '订单号（唯一）',
    homestay_id BIGINT NOT NULL COMMENT '房源ID',
    guest_id BIGINT NOT NULL COMMENT '住客（用户）ID',
    guest_phone VARCHAR(20) COMMENT '住客电话',
    check_in_date DATE NOT NULL COMMENT '入住日期',
    check_out_date DATE NOT NULL COMMENT '退房日期',
    nights INT NOT NULL COMMENT '住宿晚数',
    guest_count INT NOT NULL COMMENT '入住人数',
    price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status VARCHAR(255) NOT NULL COMMENT '订单状态',
    payment_status VARCHAR(20) COMMENT '支付状态（UNPAID/PAID/REFUNDED...）',
    payment_method VARCHAR(50) COMMENT '支付方式',
    remark TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 基础复合索引（与 Order.java 中的 @Index 对应）
    INDEX idx_homestay_dates (homestay_id, check_in_date, check_out_date)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';
