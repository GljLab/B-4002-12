CREATE TABLE IF NOT EXISTS categories (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100)    NOT NULL,
    slug       VARCHAR(100)    NOT NULL,
    parent_id  BIGINT UNSIGNED NULL,
    enabled    TINYINT(1)      NOT NULL DEFAULT 1,
    sort_order INT             NOT NULL DEFAULT 0,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_slug (slug),
    KEY idx_categories_parent_id (parent_id),
    KEY idx_categories_enabled (enabled),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (name, slug, parent_id, enabled, sort_order) VALUES
    ('默认分类', 'default', NULL, 1, 0);

CREATE TABLE IF NOT EXISTS keywords (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    usage_count INT             NOT NULL DEFAULT 0,
    last_used_at DATETIME       NULL,
    archived    TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_keywords_name (name),
    KEY idx_keywords_usage_count (usage_count),
    KEY idx_keywords_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_keywords (
    post_id    BIGINT UNSIGNED NOT NULL,
    keyword_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (post_id, keyword_id),
    KEY idx_post_keywords_keyword_id (keyword_id),
    CONSTRAINT fk_post_keywords_post    FOREIGN KEY (post_id)    REFERENCES posts(id)     ON DELETE CASCADE,
    CONSTRAINT fk_post_keywords_keyword FOREIGN KEY (keyword_id) REFERENCES keywords(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_category_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'category_id'
);

SET @add_category_id_sql := IF(
    @has_category_id = 0,
    'ALTER TABLE posts ADD COLUMN category_id BIGINT UNSIGNED NULL AFTER author_id',
    'SELECT 1'
);

PREPARE stmt FROM @add_category_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_posts_category := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND CONSTRAINT_NAME = 'fk_posts_category'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @add_fk_posts_category_sql := IF(
    @has_fk_posts_category = 0,
    'ALTER TABLE posts ADD CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL',
    'SELECT 1'
);

PREPARE stmt FROM @add_fk_posts_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @default_cat_id := (SELECT id FROM categories WHERE slug = 'default' LIMIT 1);

SET @has_cat_id_posts := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'category_id'
);

SET @update_posts_cat_sql := IF(
    @has_cat_id_posts > 0,
    'UPDATE posts SET category_id = @default_cat_id WHERE category_id IS NULL',
    'SELECT 1'
);

PREPARE stmt FROM @update_posts_cat_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_posts_category := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND INDEX_NAME = 'idx_posts_category_id'
);

SET @create_idx_posts_category_sql := IF(
    @has_idx_posts_category = 0,
    'CREATE INDEX idx_posts_category_id ON posts (category_id)',
    'SELECT 1'
);

PREPARE stmt FROM @create_idx_posts_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
