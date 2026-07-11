ALTER TABLE community_posts
    ADD COLUMN video_cover_media_file_id BIGINT;

CREATE INDEX idx_community_posts_video_cover_media_file_id
    ON community_posts (video_cover_media_file_id);
