package com.studyflow.media.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record MediaUploadPrepareResponse(
        Long mediaFileId,
        String objectKey,
        String uploadUrl,
        Map<String, String> headers,
        String contentType,
        Long maxSizeBytes,
        LocalDateTime expiresAt
) {
}
