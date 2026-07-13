CREATE TABLE community_collections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_collections_circle FOREIGN KEY (circle_id) REFERENCES circles(id),
    CONSTRAINT fk_community_collections_author FOREIGN KEY (author_id) REFERENCES users(id),
    UNIQUE KEY uk_community_collections_author_title (circle_id, author_id, title),
    INDEX idx_community_collections_author (circle_id, author_id, status)
);

ALTER TABLE community_posts
    ADD COLUMN collection_id BIGINT;

ALTER TABLE community_posts
    ADD CONSTRAINT fk_community_posts_collection FOREIGN KEY (collection_id) REFERENCES community_collections(id);

CREATE INDEX idx_community_posts_collection
    ON community_posts (collection_id, status, created_at, id);
