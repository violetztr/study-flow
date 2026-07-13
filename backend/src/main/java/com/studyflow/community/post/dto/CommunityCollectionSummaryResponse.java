package com.studyflow.community.post.dto;

import java.time.LocalDateTime;

public record CommunityCollectionSummaryResponse(
        Long id,
        String title,
        String description,
        Integer postCount,
        LocalDateTime updatedAt
) {
}
