ALTER TABLE community_posts
    ADD COLUMN favorite_count INT NOT NULL DEFAULT 0 AFTER pig_count;

CREATE TABLE community_favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_favorites_circle FOREIGN KEY (circle_id) REFERENCES circles(id),
    CONSTRAINT fk_community_favorites_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT fk_community_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_community_favorites_post_user (circle_id, post_id, user_id),
    INDEX idx_community_favorites_user_created (user_id, created_at),
    INDEX idx_community_favorites_post_id (post_id)
);
