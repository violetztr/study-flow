package com.studyflow.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GitHubRepositoryRequest(
        @NotBlank(message = "GitHub owner is required")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "GitHub owner can only contain letters, numbers, and hyphens")
        @Size(max = 80, message = "GitHub owner must be at most 80 characters")
        String owner,

        @NotBlank(message = "GitHub repo is required")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "GitHub repo can only contain letters, numbers, dots, underscores, and hyphens")
        @Size(max = 120, message = "GitHub repo must be at most 120 characters")
        String repo
) {
}
