ALTER TABLE media_files ADD COLUMN transcode_status VARCHAR(30);
ALTER TABLE media_files ADD COLUMN transcode_error VARCHAR(1000);
ALTER TABLE media_files ADD COLUMN transcode_started_at DATETIME;
ALTER TABLE media_files ADD COLUMN transcode_completed_at DATETIME;
ALTER TABLE media_files ADD COLUMN hls_master_object_key VARCHAR(500);
ALTER TABLE media_files ADD COLUMN duration_seconds INT;

UPDATE media_files
SET transcode_status = 'WAITING'
WHERE file_type = 'VIDEO'
  AND transcode_status IS NULL;

CREATE INDEX idx_media_files_transcode_status
    ON media_files (file_type, transcode_status);

CREATE TABLE media_transcode_variants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    media_file_id BIGINT NOT NULL,
    quality_label VARCHAR(30) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    bitrate_kbps INT,
    playlist_object_key VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'READY',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_transcode_variants_file FOREIGN KEY (media_file_id) REFERENCES media_files(id),
    UNIQUE KEY uk_media_transcode_variants_media_quality (media_file_id, quality_label),
    INDEX idx_media_transcode_variants_media (media_file_id)
);

CREATE TABLE media_transcode_segments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    media_file_id BIGINT NOT NULL,
    quality_label VARCHAR(30) NOT NULL,
    segment_index INT NOT NULL,
    duration_seconds DECIMAL(8, 3) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    byte_size BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_transcode_segments_file FOREIGN KEY (media_file_id) REFERENCES media_files(id),
    UNIQUE KEY uk_media_transcode_segments_media_quality_index (media_file_id, quality_label, segment_index),
    INDEX idx_media_transcode_segments_media_quality (media_file_id, quality_label)
);
