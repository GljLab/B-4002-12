ALTER TABLE comments 
    ADD COLUMN parent_id BIGINT NULL,
    ADD COLUMN reply_to_comment_id BIGINT NULL,
    ADD COLUMN reply_to_user_id BIGINT UNSIGNED NULL,
    ADD COLUMN depth INT NOT NULL DEFAULT 0,
    ADD COLUMN path VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
    ADD COLUMN upvotes INT NOT NULL DEFAULT 0,
    ADD COLUMN downvotes INT NOT NULL DEFAULT 0,
    ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN is_edited TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN edited_at DATETIME NULL,
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at DATETIME NULL,
    ADD COLUMN deleted_by VARCHAR(50) NULL,
    ADD COLUMN has_images TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN has_code TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN mentions TEXT NULL,
    ADD COLUMN version INT NOT NULL DEFAULT 1,
    ADD CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_comments_reply_to_comment FOREIGN KEY (reply_to_comment_id) REFERENCES comments(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_comments_reply_to_user FOREIGN KEY (reply_to_user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD INDEX idx_comments_post_deleted (post_id, is_deleted),
    ADD INDEX idx_comments_post_parent (post_id, parent_id, is_deleted),
    ADD INDEX idx_comments_path (path),
    ADD INDEX idx_comments_user_deleted (user_id, is_deleted),
    ADD INDEX idx_comments_pinned (post_id, is_pinned, is_deleted);

CREATE TABLE comment_votes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    vote_type TINYINT NOT NULL COMMENT '1=upvote, -1=downvote',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_votes_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_votes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_comment_vote (comment_id, user_id),
    INDEX idx_comment_votes_comment (comment_id),
    INDEX idx_comment_votes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment_reports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, REVIEWED, RESOLVED, REJECTED',
    reviewed_by BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    resolution TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT fk_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_comment_reports_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
    INDEX idx_comment_reports_comment (comment_id),
    INDEX idx_comment_reports_reporter (reporter_id),
    INDEX idx_comment_reports_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(30) NOT NULL COMMENT 'REPLY, MENTION, UPVOTE, REPORT_RESULT',
    related_comment_id BIGINT NULL,
    related_post_id BIGINT UNSIGNED NULL,
    related_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_comment FOREIGN KEY (related_comment_id) REFERENCES comments(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_post FOREIGN KEY (related_post_id) REFERENCES posts(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_related_user FOREIGN KEY (related_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_user_created (user_id, created_at),
    INDEX idx_notifications_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sensitive_words (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL' COMMENT 'POLITICS, PORNOGRAPHY, VIOLENCE, ADVERTISEMENT, GENERAL',
    severity TINYINT NOT NULL DEFAULT 1 COMMENT '1=warn, 2=block, 3=auto-report',
    replacement VARCHAR(100) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    INDEX idx_sensitive_words_active (is_active),
    INDEX idx_sensitive_words_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_comment_settings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT UNSIGNED NOT NULL UNIQUE,
    access_rule VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, SUBSCRIBERS_ONLY, CLOSED',
    require_approval TINYINT(1) NOT NULL DEFAULT 0,
    max_depth INT NOT NULL DEFAULT 3,
    max_length INT NOT NULL DEFAULT 2000,
    allow_images TINYINT(1) NOT NULL DEFAULT 1,
    allow_code TINYINT(1) NOT NULL DEFAULT 1,
    allow_emojis TINYINT(1) NOT NULL DEFAULT 1,
    allow_mentions TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT fk_post_comment_settings_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment_rate_limits (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    window_start DATETIME NOT NULL,
    count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_rate_limits_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_window (user_id, window_start),
    INDEX idx_rate_limits_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sensitive_words (word, category, severity, replacement) VALUES
('敏感词1', 'GENERAL', 1, '***'),
('敏感词2', 'GENERAL', 2, '***'),
('广告', 'ADVERTISEMENT', 1, '***');
