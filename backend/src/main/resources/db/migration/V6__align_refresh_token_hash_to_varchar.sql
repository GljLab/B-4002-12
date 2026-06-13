SET @refresh_tokens_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
);

SET @token_hash_column_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
      AND COLUMN_NAME = 'token_hash'
    LIMIT 1
);

SET @alter_token_hash_sql := IF(
    @refresh_tokens_table_exists = 1
    AND @token_hash_column_type IS NOT NULL
    AND @token_hash_column_type <> 'varchar(64)',
    'ALTER TABLE refresh_tokens MODIFY COLUMN token_hash VARCHAR(64) NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @alter_token_hash_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
