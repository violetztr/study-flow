package com.studyflow.community.danmaku.dto;

import java.time.LocalDateTime;

public record CommunityDanmakuResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        Integer timeSeconds,
        String color,
        LocalDateTime createdAt
) {
}
