package com.studyflow.project.dto;

import com.studyflow.project.Project;

public record ProjectResponse(Long id, String name, String description, String status) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus()
        );
    }
}
