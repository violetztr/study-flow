package com.studyflow.media.dto;

import java.util.List;

public record MediaAttachmentResponse(
        Long id,
        String fileType,
        String contentType,
        String originalFilename,
        Long fileSize,
        String url,
        String coverUrl,
        String playbackUrl,
        String playbackType,
        String transcodeStatus,
        String transcodeError,
        List<MediaTranscodeVariantResponse> qualities
) {
}
