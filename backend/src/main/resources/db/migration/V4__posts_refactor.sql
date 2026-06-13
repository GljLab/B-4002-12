SET @has_deleted_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'deleted_at'
);

SET @add_deleted_at_sql := IF(
    @has_deleted_at = 0,
    'ALTER TABLE posts ADD COLUMN deleted_at DATETIME NULL',
    'SELECT 1'
);

PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_version := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'version'
);

SET @add_version_sql := IF(
    @has_version = 0,
    'ALTER TABLE posts ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'
);

PREPARE stmt FROM @add_version_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_posts_deleted_at := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND INDEX_NAME = 'idx_posts_deleted_at'
);

SET @create_idx_posts_deleted_at_sql := IF(
    @has_idx_posts_deleted_at = 0,
    'CREATE INDEX idx_posts_deleted_at ON posts (deleted_at)',
    'SELECT 1'
);

PREPARE stmt FROM @create_idx_posts_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
