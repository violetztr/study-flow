package com.studyflow.community.background.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BackgroundPresetRequest(
        @NotBlank
        @Size(max = 20)
        String placement,
        @NotBlank
        @Size(max = 80)
        String name,
        @NotBlank
        @Size(max = 500)
        String url,
        @NotBlank
        @Size(max = 20)
        String mediaType
) {
}
