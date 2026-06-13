ALTER TABLE posts ADD COLUMN scheduled_at DATETIME NULL AFTER view_count;
ALTER TABLE posts ADD COLUMN parent_post_id BIGINT NULL AFTER scheduled_at;
ALTER TABLE posts ADD COLUMN is_revision TINYINT(1) NOT NULL DEFAULT 0 AFTER parent_post_id;

CREATE INDEX idx_posts_scheduled_at ON posts (scheduled_at);
CREATE INDEX idx_posts_parent_post_id ON posts (parent_post_id);
CREATE INDEX idx_posts_is_revision ON posts (is_revision);
CREATE INDEX idx_posts_status_scheduled ON posts (status, scheduled_at);

CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    width INT NULL,
    height INT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE INDEX idx_images_author_id ON images (author_id);
CREATE INDEX idx_images_filename ON images (filename);
