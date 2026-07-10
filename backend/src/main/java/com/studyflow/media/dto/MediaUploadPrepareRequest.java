package com.studyflow.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MediaUploadPrepareRequest(
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 120) String contentType,
        @NotNull @Positive Long fileSize
) {
}
