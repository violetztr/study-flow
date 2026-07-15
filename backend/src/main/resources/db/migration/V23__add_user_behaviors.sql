-- V23: add user behavior tracking table
CREATE TABLE IF NOT EXISTS user_behaviors
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NULL COMMENT 'NULL for anonymous users',
    target_type VARCHAR(20) NOT NULL COMMENT 'POST, TOPIC',
    target_id   BIGINT      NULL COMMENT 'NULL for SEARCH actions',
    action     VARCHAR(20) NOT NULL COMMENT 'VIEW, LIKE, FAVORITE, FOLLOW, SEARCH',
    extra      VARCHAR(500) NULL COMMENT 'e.g. search keyword for SEARCH action',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_target_action (target_type, target_id, action),
    INDEX idx_created (created_at)
);
