ALTER TABLE community_posts
    ADD COLUMN reviewed_by BIGINT;

ALTER TABLE community_posts
    ADD COLUMN reviewed_at DATETIME;

ALTER TABLE community_posts
    ADD COLUMN review_reason VARCHAR(500);

CREATE INDEX idx_community_posts_circle_status_created
    ON community_posts (circle_id, status, created_at);

CREATE INDEX idx_community_posts_author_status_created
    ON community_posts (author_id, status, created_at);

UPDATE users
SET role = 'ADMIN'
WHERE username = 'ruru';
