ALTER TABLE read_history ADD COLUMN duration_seconds INT NOT NULL DEFAULT 0 AFTER read_at;
ALTER TABLE read_history ADD COLUMN scroll_position INT NOT NULL DEFAULT 0 AFTER duration_seconds;

ALTER TABLE favorites ADD COLUMN note VARCHAR(500) NULL AFTER created_at;

CREATE TABLE collection_albums (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    cover_url VARCHAR(500) NULL,
    is_public TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    share_token VARCHAR(32) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_collection_albums_user_id ON collection_albums (user_id);
CREATE UNIQUE INDEX idx_collection_albums_share_token ON collection_albums (share_token);

CREATE TABLE collection_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT UNSIGNED NOT NULL,
    post_id BIGINT UNSIGNED NOT NULL,
    note VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (album_id) REFERENCES collection_albums(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    UNIQUE KEY uk_collection_items_album_post (album_id, post_id)
);

CREATE INDEX idx_collection_items_album_id ON collection_items (album_id);
CREATE INDEX idx_collection_items_post_id ON collection_items (post_id);
