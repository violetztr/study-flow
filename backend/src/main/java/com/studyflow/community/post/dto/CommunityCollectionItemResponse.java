package com.studyflow.community.post.dto;

import java.time.LocalDateTime;

public record CommunityCollectionItemResponse(
        Long postId,
        String title,
        String contentType,
        Integer viewCount,
        LocalDateTime createdAt
) {
}
