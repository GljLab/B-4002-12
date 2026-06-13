SET @users_id_column_type := (
    SELECT COALESCE(COLUMN_TYPE, 'BIGINT UNSIGNED')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'id'
    LIMIT 1
);

SET @refresh_tokens_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
);

SET @create_refresh_tokens_sql := IF(
    @refresh_tokens_exists = 0,
    CONCAT(
        'CREATE TABLE refresh_tokens (',
        'id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,',
        'user_id ', @users_id_column_type, ' NOT NULL,',
        'token_hash CHAR(64) NOT NULL,',
        'expires_at DATETIME NOT NULL,',
        'revoked TINYINT(1) NOT NULL DEFAULT 0,',
        'created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,',
        'CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,',
        'UNIQUE KEY uk_refresh_tokens_hash (token_hash),',
        'INDEX idx_refresh_tokens_user_id (user_id),',
        'INDEX idx_refresh_tokens_expires_at (expires_at)',
        ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    ),
    'SELECT 1'
);

PREPARE stmt FROM @create_refresh_tokens_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
