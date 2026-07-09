package com.studyflow.project.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PortfolioProjectRequest(
        @NotBlank(message = "Portfolio slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Portfolio slug must use lowercase letters, numbers, and hyphens")
        @Size(max = 120, message = "Portfolio slug must be at most 120 characters")
        String slug,

        Boolean publicVisible,

        Boolean featured,

        Integer displayOrder,

        @Size(max = 500, message = "Public summary must be at most 500 characters")
        String publicSummary
) {
}
