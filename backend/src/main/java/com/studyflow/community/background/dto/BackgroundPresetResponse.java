package com.studyflow.community.background.dto;

import java.time.LocalDateTime;

public record BackgroundPresetResponse(
        Long id,
        String placement,
        String name,
        String url,
        String mediaType,
        Boolean systemProvided,
        Integer sortOrder,
        LocalDateTime createdAt
) {
}
