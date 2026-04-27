ALTER TABLE homestay_availability
    ADD COLUMN source VARCHAR(30) NULL COMMENT '占用来源: ORDER/MANUAL/SYSTEM/MAINTENANCE',
    ADD COLUMN reason VARCHAR(255) NULL COMMENT '不可用原因',
    ADD COLUMN note TEXT NULL COMMENT '房东日历备注',
    ADD COLUMN created_by BIGINT NULL COMMENT '操作人用户ID';

CREATE INDEX idx_availability_source ON homestay_availability (source);
