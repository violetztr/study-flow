package com.studyflow.project.profile.dto;

import com.studyflow.project.profile.ProjectTechStack;

public record ProjectTechStackResponse(
        Long id,
        String name,
        String category,
        Integer sortOrder
) {
    public static ProjectTechStackResponse from(ProjectTechStack techStack) {
        return new ProjectTechStackResponse(
                techStack.getId(),
                techStack.getName(),
                techStack.getCategory(),
                techStack.getSortOrder()
        );
    }
}
