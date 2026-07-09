package com.studyflow.community.comment.dto;

import java.time.LocalDateTime;

public record CommunityCommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        String status,
        Integer reactionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
