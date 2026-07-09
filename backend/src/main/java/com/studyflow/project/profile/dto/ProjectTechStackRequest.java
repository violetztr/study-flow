package com.studyflow.project.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectTechStackRequest(
        @NotBlank(message = "Tech stack name is required")
        @Size(max = 60, message = "Tech stack name must be at most 60 characters")
        String name,

        @Size(max = 40, message = "Tech stack category must be at most 40 characters")
        String category,

        Integer sortOrder
) {
}
