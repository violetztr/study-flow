CREATE TABLE live_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    color VARCHAR(20) DEFAULT '#ffffff',
    type VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_live_messages_room FOREIGN KEY (room_id) REFERENCES live_rooms(id),
    CONSTRAINT fk_live_messages_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_live_messages_room_time (room_id, created_at),
    INDEX idx_live_messages_user (user_id)
);
