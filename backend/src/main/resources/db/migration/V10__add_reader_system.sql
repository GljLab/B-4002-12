ALTER TABLE users ADD COLUMN reader_level INT NOT NULL DEFAULT 1 AFTER last_login_at;
ALTER TABLE users ADD COLUMN reader_exp INT NOT NULL DEFAULT 0 AFTER reader_level;
ALTER TABLE users ADD COLUMN security_question VARCHAR(200) NULL AFTER reader_exp;
ALTER TABLE users ADD COLUMN security_answer VARCHAR(255) NULL AFTER security_question;
ALTER TABLE users ADD COLUMN show_footprint TINYINT(1) NOT NULL DEFAULT 1 AFTER security_answer;
ALTER TABLE users ADD COLUMN streak_days INT NOT NULL DEFAULT 0 AFTER show_footprint;
ALTER TABLE users ADD COLUMN last_active_date DATE NULL AFTER streak_days;

CREATE TABLE read_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    post_id BIGINT UNSIGNED NOT NULL,
    read_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE INDEX idx_read_history_user_id ON read_history (user_id);
CREATE INDEX idx_read_history_post_id ON read_history (post_id);
CREATE INDEX idx_read_history_user_read ON read_history (user_id, read_at DESC);

CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    post_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    UNIQUE KEY uk_favorites_user_post (user_id, post_id)
);

CREATE INDEX idx_favorites_user_id ON favorites (user_id);

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    post_id BIGINT UNSIGNED NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE INDEX idx_comments_user_id ON comments (user_id);
CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_created ON comments (created_at DESC);

CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reader_id BIGINT UNSIGNED NOT NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (reader_id) REFERENCES users(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    UNIQUE KEY uk_subscriptions_reader_author (reader_id, author_id)
);

CREATE INDEX idx_subscriptions_reader_id ON subscriptions (reader_id);
CREATE INDEX idx_subscriptions_author_id ON subscriptions (author_id);

CREATE TABLE captcha_store (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0
);

CREATE INDEX idx_captcha_created ON captcha_store (created_at);

CREATE TABLE registration_rate_limit (
    ip_hash VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_reg_rate_ip (ip_hash, created_at)
);
