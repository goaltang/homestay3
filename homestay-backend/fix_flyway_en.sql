-- Fix Flyway migration status
-- Delete failed V14 migration record
DELETE FROM flyway_schema_history WHERE version = '14' AND success = 0;

-- Check and create payment_records table if not exists
CREATE TABLE IF NOT EXISTS payment_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    out_trade_no VARCHAR(64) NOT NULL UNIQUE,
    transaction_id VARCHAR(64),
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_params TEXT,
    response_params TEXT,
    notify_params TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_order_id (order_id),
    INDEX idx_out_trade_no (out_trade_no),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_payment_method (payment_method),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Check and add payment_status column if not exists
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'payment_status';

SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20) DEFAULT ''UNPAID'' AFTER status',
  'SELECT ''payment_status column already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add index if not exists
SET @idx_exists = 0;
SELECT COUNT(*) INTO @idx_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'idx_payment_status';

SET @sql = IF(@idx_exists = 0, 
  'ALTER TABLE orders ADD INDEX idx_payment_status (payment_status)',
  'SELECT ''idx_payment_status index already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Insert successful V14 migration record
INSERT INTO flyway_schema_history (
    installed_rank, 
    version, 
    description, 
    type, 
    script, 
    checksum, 
    installed_by, 
    installed_on, 
    execution_time, 
    success
) VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history AS fsh), 
    '14', 
    'add payment records table', 
    'SQL', 
    'V14__add_payment_records_table.sql', 
    NULL, 
    'root', 
    NOW(), 
    1000, 
    1
);

SELECT 'Flyway migration V14 fixed successfully' as message; 