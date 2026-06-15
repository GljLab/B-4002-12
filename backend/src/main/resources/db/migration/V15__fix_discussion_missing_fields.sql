ALTER TABLE comments
    ADD COLUMN updated_at DATETIME NULL AFTER created_at;

ALTER TABLE comment_rate_limits
    ADD COLUMN updated_at DATETIME NULL AFTER created_at;
