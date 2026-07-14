package com.studyflow.media.dto;

public record MediaAttachmentResponse(
        Long id,
        String fileType,
        String contentType,
        String originalFilename,
        Long fileSize,
        String url,
        String coverUrl
) {
}
