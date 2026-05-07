CREATE TABLE IF NOT EXISTS notification_broadcast_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(30) NOT NULL COMMENT 'Broadcast job status',
    initiated_by BIGINT NULL COMMENT 'Admin user id when available',
    initiated_by_username VARCHAR(100) NULL COMMENT 'Admin username when available',
    content_summary VARCHAR(255) NOT NULL COMMENT 'Trimmed content summary',
    content_length INT NOT NULL COMMENT 'Trimmed content length',
    target_count INT NOT NULL DEFAULT 0 COMMENT 'Enabled recipient count',
    success_count INT NOT NULL DEFAULT 0 COMMENT 'Created notification count',
    failure_count INT NOT NULL DEFAULT 0 COMMENT 'Failed notification count',
    failure_reason VARCHAR(1000) NULL COMMENT 'Failure or rate limit reason',
    submitted_at DATETIME NOT NULL COMMENT 'Submitted time',
    started_at DATETIME NULL COMMENT 'Processing start time',
    completed_at DATETIME NULL COMMENT 'Processing completion time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    INDEX idx_notification_broadcast_status_submitted (status, submitted_at),
    INDEX idx_notification_broadcast_initiator_submitted (initiated_by, submitted_at),
    CONSTRAINT fk_notification_broadcast_jobs_initiator FOREIGN KEY (initiated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification broadcast jobs';
