package com.studyflow.media.dto;

public record MediaUploadCompleteResponse(
        Long id,
        String fileType,
        String contentType,
        String originalFilename,
        Long fileSize,
        String status
) {
}
