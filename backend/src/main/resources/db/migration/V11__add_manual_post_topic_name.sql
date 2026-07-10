ALTER TABLE community_posts
    ADD COLUMN topic_name VARCHAR(80);

UPDATE community_posts p
SET topic_name = (
    SELECT t.name
    FROM community_topics t
    WHERE t.id = p.topic_id
)
WHERE p.topic_id IS NOT NULL
  AND p.topic_name IS NULL;
