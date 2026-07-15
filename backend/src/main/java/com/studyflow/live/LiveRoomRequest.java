package com.studyflow.live;

import java.time.LocalDateTime;

public record LiveRoomRequest(
        String title,
        String coverUrl,
        Long topicId,
        String topicName
) {
}
