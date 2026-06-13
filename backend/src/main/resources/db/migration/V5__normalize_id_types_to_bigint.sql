SET @posts_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND CONSTRAINT_NAME = 'fk_posts_author'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @drop_posts_fk_sql := IF(
    @posts_fk_exists = 1,
    'ALTER TABLE posts DROP FOREIGN KEY fk_posts_author',
    'SELECT 1'
);

PREPARE stmt FROM @drop_posts_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @refresh_tokens_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
);

SET @refresh_tokens_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
      AND CONSTRAINT_NAME = 'fk_refresh_tokens_user'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @drop_refresh_tokens_fk_sql := IF(
    @refresh_tokens_table_exists = 1 AND @refresh_tokens_fk_exists = 1,
    'ALTER TABLE refresh_tokens DROP FOREIGN KEY fk_refresh_tokens_user',
    'SELECT 1'
);

PREPARE stmt FROM @drop_refresh_tokens_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @users_id_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'id'
    LIMIT 1
);

SET @alter_users_id_sql := IF(
    @users_id_type <> 'bigint unsigned',
    'ALTER TABLE users MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);

PREPARE stmt FROM @alter_users_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @posts_id_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'id'
    LIMIT 1
);

SET @alter_posts_id_sql := IF(
    @posts_id_type <> 'bigint unsigned',
    'ALTER TABLE posts MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);

PREPARE stmt FROM @alter_posts_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @posts_author_id_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'author_id'
    LIMIT 1
);

SET @alter_posts_author_id_sql := IF(
    @posts_author_id_type <> 'bigint unsigned',
    'ALTER TABLE posts MODIFY COLUMN author_id BIGINT UNSIGNED NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @alter_posts_author_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @refresh_tokens_user_id_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
      AND COLUMN_NAME = 'user_id'
    LIMIT 1
);

SET @alter_refresh_tokens_user_id_sql := IF(
    @refresh_tokens_table_exists = 1 AND @refresh_tokens_user_id_type <> 'bigint unsigned',
    'ALTER TABLE refresh_tokens MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @alter_refresh_tokens_user_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @posts_fk_exists_after := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND CONSTRAINT_NAME = 'fk_posts_author'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @create_posts_fk_sql := IF(
    @posts_fk_exists_after = 0,
    'ALTER TABLE posts ADD CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT 1'
);

PREPARE stmt FROM @create_posts_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @refresh_tokens_fk_exists_after := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_tokens'
      AND CONSTRAINT_NAME = 'fk_refresh_tokens_user'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @create_refresh_tokens_fk_sql := IF(
    @refresh_tokens_table_exists = 1 AND @refresh_tokens_fk_exists_after = 0,
    'ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT 1'
);

PREPARE stmt FROM @create_refresh_tokens_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
