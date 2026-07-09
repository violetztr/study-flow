package com.studyflow.community.post.dto;

import java.time.LocalDateTime;

public record CommunityPostResponse(
        Long id,
        Long circleId,
        Long authorId,
        String authorName,
        Long topicId,
        String topicName,
        String title,
        String content,
        String status,
        Boolean pinned,
        Integer commentCount,
        Integer reactionCount,
        Integer viewCount,
        Boolean likedByCurrentUser,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
