package com.studyflow.community.view.dto;

import com.studyflow.community.post.dto.CommunityPostResponse;

import java.time.LocalDateTime;

public record CommunityWatchHistoryResponse(
        CommunityPostResponse post,
        Integer maxProgressSeconds,
        Integer durationSeconds,
        LocalDateTime firstViewedAt,
        LocalDateTime lastViewedAt
) {
}
