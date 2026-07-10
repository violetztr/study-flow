package com.studyflow.community.post.dto;

import com.studyflow.media.dto.MediaAttachmentResponse;

import java.time.LocalDateTime;
import java.util.List;

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
        List<MediaAttachmentResponse> media,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
