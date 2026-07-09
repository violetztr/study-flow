package com.studyflow.portfolio.dto;

import com.studyflow.github.dto.GitHubRepositoryResponse;
import com.studyflow.project.Project;
import com.studyflow.project.profile.PortfolioProject;
import com.studyflow.project.profile.ProjectProfile;
import com.studyflow.project.profile.dto.ProjectTechStackResponse;

import java.util.List;

public record PublicPortfolioProjectResponse(
        Long projectId,
        String name,
        String description,
        String status,
        String slug,
        Boolean featured,
        Integer displayOrder,
        String publicSummary,
        String headline,
        String productionUrl,
        String apiDocUrl,
        String databaseDocUrl,
        String architectureSummary,
        String interviewHighlights,
        String coverImageUrl,
        GitHubRepositoryResponse githubRepository,
        List<ProjectTechStackResponse> techStacks
) {
    public static PublicPortfolioProjectResponse from(
            Project project,
            PortfolioProject portfolioProject,
            ProjectProfile profile,
            List<ProjectTechStackResponse> techStacks,
            GitHubRepositoryResponse githubRepository
    ) {
        return new PublicPortfolioProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                portfolioProject.getSlug(),
                portfolioProject.getFeatured(),
                portfolioProject.getDisplayOrder(),
                portfolioProject.getPublicSummary(),
                profile == null ? null : profile.getHeadline(),
                profile == null ? null : profile.getProductionUrl(),
                profile == null ? null : profile.getApiDocUrl(),
                profile == null ? null : profile.getDatabaseDocUrl(),
                profile == null ? null : profile.getArchitectureSummary(),
                profile == null ? null : profile.getInterviewHighlights(),
                profile == null ? null : profile.getCoverImageUrl(),
                githubRepository,
                techStacks
        );
    }
}
