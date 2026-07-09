package com.studyflow.community.moderation.dto;

import jakarta.validation.constraints.Size;

public record ModerationRequest(
        @Size(max = 500) String reason
) {
}
