ALTER TABLE user_profiles
    ADD COLUMN home_background_url VARCHAR(500);

ALTER TABLE user_profiles
    ADD COLUMN home_background_type VARCHAR(20) DEFAULT 'VIDEO';

CREATE TABLE background_presets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    placement VARCHAR(20) NOT NULL,
    name VARCHAR(80) NOT NULL,
    url VARCHAR(500) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    system_provided BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_background_presets_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_background_presets_placement_sort (placement, sort_order, id)
);

INSERT INTO background_presets (placement, name, url, media_type, system_provided, sort_order)
VALUES
    ('HOME', 'Wilderness', '/system-backgrounds/site/home-hero.mp4', 'VIDEO', TRUE, 10),
    ('PROFILE', 'Road', '/system-backgrounds/profile/road.png', 'IMAGE', TRUE, 10),
    ('PROFILE', 'Silhouette', '/system-backgrounds/profile/silhouette.mp4', 'VIDEO', TRUE, 20),
    ('PROFILE', 'Cabin', '/system-backgrounds/profile/cabin.mp4', 'VIDEO', TRUE, 30);
