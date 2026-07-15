package com.studyflow.live;

import java.time.LocalDateTime;

public record LiveRoomResponse(
        Long id,
        Long userId,
        String username,
        String userAvatarUrl,
        Long circleId,
        String title,
        String coverUrl,
        Long topicId,
        String topicName,
        String streamKey,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer peakViewers,
        Integer totalViews,
        Integer currentViewers,
        String flvUrl,
        String hlsUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
