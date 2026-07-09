package com.studyflow.community.member.dto;

public record UserProfileRequest(
        String displayName,
        String bio,
        String skills,
        String githubUrl,
        String websiteUrl
) {
}
