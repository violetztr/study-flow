package com.studyflow.community.view.dto;

import jakarta.validation.constraints.Min;

public record CommunityViewReportRequest(
        @Min(0) Integer playedSeconds,
        @Min(0) Integer durationSeconds
) {
}
