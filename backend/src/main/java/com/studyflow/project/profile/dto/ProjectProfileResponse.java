package com.studyflow.project.profile.dto;

import com.studyflow.project.profile.ProjectProfile;

public record ProjectProfileResponse(
        Long id,
        Long projectId,
        String headline,
        String productionUrl,
        String apiDocUrl,
        String databaseDocUrl,
        String architectureSummary,
        String interviewHighlights,
        String coverImageUrl
) {
    public static ProjectProfileResponse empty(Long projectId) {
        return new ProjectProfileResponse(
                null,
                projectId,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static ProjectProfileResponse from(ProjectProfile profile) {
        return new ProjectProfileResponse(
                profile.getId(),
                profile.getProjectId(),
                profile.getHeadline(),
                profile.getProductionUrl(),
                profile.getApiDocUrl(),
                profile.getDatabaseDocUrl(),
                profile.getArchitectureSummary(),
                profile.getInterviewHighlights(),
                profile.getCoverImageUrl()
        );
    }
}
