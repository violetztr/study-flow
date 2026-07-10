CREATE TABLE user_wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pig_balance INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_wallets_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_wallets_user (user_id)
);

CREATE TABLE user_daily_rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reward_date DATE NOT NULL,
    reward_type VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_daily_rewards_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_daily_rewards_user_date_type (user_id, reward_date, reward_type)
);

CREATE TABLE user_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users(id),
    CONSTRAINT fk_user_follows_following FOREIGN KEY (following_id) REFERENCES users(id),
    UNIQUE KEY uk_user_follows_pair (follower_id, following_id),
    INDEX idx_user_follows_following (following_id),
    INDEX idx_user_follows_follower (follower_id)
);

CREATE TABLE community_danmaku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    time_seconds INT NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#ffffff',
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_danmaku_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT fk_community_danmaku_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_community_danmaku_post_time (post_id, time_seconds, id),
    INDEX idx_community_danmaku_user (user_id)
);
