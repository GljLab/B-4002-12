SET @has_updated_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'updated_at'
);

SET @ddl := IF(
    @has_updated_at = 0,
    'ALTER TABLE posts ADD COLUMN updated_at DATETIME NULL',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE posts
SET updated_at = created_at
WHERE updated_at IS NULL;
