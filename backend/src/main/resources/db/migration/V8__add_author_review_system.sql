-- ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN' AFTER password;
-- ALTER TABLE users ADD COLUMN nickname VARCHAR(100) NULL AFTER role;
-- ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL AFTER nickname;
-- ALTER TABLE users ADD COLUMN bio TEXT NULL AFTER avatar_url;
-- ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER bio;
-- ALTER TABLE users ADD COLUMN login_attempts INT NOT NULL DEFAULT 0 AFTER enabled;
-- ALTER TABLE users ADD COLUMN locked_until DATETIME NULL AFTER login_attempts;
-- ALTER TABLE users ADD COLUMN last_login_at DATETIME NULL AFTER locked_until;

-- UPDATE users SET role = 'ADMIN', nickname = username WHERE role = 'ADMIN';

-- ALTER TABLE posts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' AFTER content;
-- ALTER TABLE posts ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER status;
-- ALTER TABLE posts ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 AFTER rejection_reason;

-- UPDATE posts SET status = 'PUBLISHED' WHERE status = 'PUBLISHED';

-- CREATE INDEX idx_posts_status ON posts (status);
-- CREATE INDEX idx_users_role ON users (role);
-- CREATE INDEX idx_users_enabled ON users (enabled);
select 1;