ALTER TABLE community_posts
    ADD COLUMN pig_count INT NOT NULL DEFAULT 0 AFTER reaction_count;
