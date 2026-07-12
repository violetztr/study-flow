package com.studyflow.media.dto;

public record MediaTranscodeVariantResponse(
        String qualityLabel,
        Integer width,
        Integer height,
        Integer bitrateKbps,
        String playlistUrl
) {
}
