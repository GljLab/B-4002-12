-- 删除旧的registration_rate_limit表并重建（添加id主键）
DROP TABLE IF EXISTS registration_rate_limit;

CREATE TABLE registration_rate_limit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_hash VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_reg_rate_ip (ip_hash, created_at)
);
