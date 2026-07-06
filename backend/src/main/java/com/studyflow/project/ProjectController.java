package com.studyflow.project;

import com.studyflow.common.ApiResponse;
import com.studyflow.project.dto.ProjectRequest;
import com.studyflow.project.dto.ProjectResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(projectService.listProjects(principal.userId()));
    }

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProjectRequest request
    ) {
        return ApiResponse.success(projectService.createProject(principal.userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> updateProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request
    ) {
        return ApiResponse.success(projectService.updateProject(principal.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        projectService.deleteProject(principal.userId(), id);
        return ApiResponse.success();
    }
}
