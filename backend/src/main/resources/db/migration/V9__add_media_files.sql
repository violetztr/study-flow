CREATE TABLE media_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uploader_id BIGINT NOT NULL,
    storage_provider VARCHAR(30) NOT NULL DEFAULT 'R2',
    bucket_name VARCHAR(120) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_type VARCHAR(30) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    uploaded_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_files_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    UNIQUE KEY uk_media_files_object_key (object_key),
    INDEX idx_media_files_uploader_status (uploader_id, status)
);

CREATE TABLE community_post_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    media_file_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_post_media_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT fk_community_post_media_file FOREIGN KEY (media_file_id) REFERENCES media_files(id),
    UNIQUE KEY uk_community_post_media_post_file (post_id, media_file_id),
    INDEX idx_community_post_media_post (post_id, sort_order)
);
