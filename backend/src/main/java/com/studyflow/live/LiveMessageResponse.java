package com.studyflow.live;

import java.time.LocalDateTime;

public record LiveMessageResponse(
        Long id,
        Long roomId,
        Long userId,
        String username,
        String content,
        String color,
        String type,
        LocalDateTime createdAt
) {
}
