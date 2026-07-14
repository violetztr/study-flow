package com.studyflow.media.dto;

import java.util.Collections;
import java.util.List;

public record MediaUploadCompleteResponse(
        Long id,
        String fileType,
        String contentType,
        String originalFilename,
        Long fileSize,
        String url,
        String status,
        String transcodeStatus,
        String transcodeError,
        List<MediaTranscodeVariantResponse> transcodeVariants
) {
    public MediaUploadCompleteResponse {
        if (transcodeVariants == null) {
            transcodeVariants = Collections.emptyList();
        }
    }

    public MediaUploadCompleteResponse(
            Long id,
            String fileType,
            String contentType,
            String originalFilename,
            Long fileSize,
            String url,
            String status,
            String transcodeStatus,
            String transcodeError
    ) {
        this(id, fileType, contentType, originalFilename, fileSize, url, status, transcodeStatus, transcodeError, Collections.emptyList());
    }
}
