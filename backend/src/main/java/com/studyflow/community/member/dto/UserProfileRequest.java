package com.studyflow.community.member.dto;

import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @Size(max = 80)
        String displayName,
        @Size(max = 500)
        String bio,
        @Size(max = 500)
        String avatarUrl,
        @Size(max = 500)
        String skills,
        @Size(max = 300)
        String githubUrl,
        @Size(max = 300)
        String websiteUrl
) {
}
