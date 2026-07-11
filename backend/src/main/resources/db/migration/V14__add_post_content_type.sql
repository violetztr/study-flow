ALTER TABLE community_posts
    ADD COLUMN content_type VARCHAR(30) NOT NULL DEFAULT 'ARTICLE';

UPDATE community_posts
SET content_type = 'VIDEO'
WHERE id IN (
    SELECT post_id
    FROM community_post_media
    WHERE media_file_id IN (
        SELECT id
        FROM media_files
        WHERE file_type = 'VIDEO'
    )
);

CREATE INDEX idx_community_posts_circle_content_activity
    ON community_posts (circle_id, content_type, pinned, last_activity_at);
