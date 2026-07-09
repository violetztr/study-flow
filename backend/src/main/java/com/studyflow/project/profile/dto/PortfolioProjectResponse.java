package com.studyflow.project.profile.dto;

import com.studyflow.project.profile.PortfolioProject;

public record PortfolioProjectResponse(
        Long id,
        Long projectId,
        String slug,
        Boolean publicVisible,
        Boolean featured,
        Integer displayOrder,
        String publicSummary
) {
    public static PortfolioProjectResponse from(PortfolioProject portfolioProject) {
        return new PortfolioProjectResponse(
                portfolioProject.getId(),
                portfolioProject.getProjectId(),
                portfolioProject.getSlug(),
                portfolioProject.getPublicVisible(),
                portfolioProject.getFeatured(),
                portfolioProject.getDisplayOrder(),
                portfolioProject.getPublicSummary()
        );
    }
}
