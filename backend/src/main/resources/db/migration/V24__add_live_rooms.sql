-- V24: add live rooms table for Phase 7.2
CREATE TABLE IF NOT EXISTS live_rooms
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL COMMENT '主播 user id',
    circle_id    BIGINT       NOT NULL COMMENT '所属圈子',
    title        VARCHAR(200) NOT NULL COMMENT '直播标题',
    cover_url    VARCHAR(500) NULL COMMENT '封面图',
    topic_id     BIGINT       NULL COMMENT '关联话题',
    topic_name   VARCHAR(100) NULL COMMENT '话题名（冗余）',
    stream_key   VARCHAR(64)  NOT NULL COMMENT '推流密钥',
    status       VARCHAR(20)  NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING / LIVE / ENDED',
    started_at   DATETIME     NULL COMMENT '开播时间',
    ended_at     DATETIME     NULL COMMENT '下播时间',
    peak_viewers INT          NOT NULL DEFAULT 0 COMMENT '峰值在线人数',
    total_views  INT          NOT NULL DEFAULT 0 COMMENT '累计观看人次',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stream_key (stream_key),
    INDEX idx_circle_status (circle_id, status),
    INDEX idx_live_room_user_created (user_id, created_at)
);
