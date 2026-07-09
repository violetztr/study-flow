package com.studyflow.project.profile;

import com.studyflow.common.ApiResponse;
import com.studyflow.project.profile.dto.PortfolioProjectRequest;
import com.studyflow.project.profile.dto.PortfolioProjectResponse;
import com.studyflow.project.profile.dto.ProjectProfileRequest;
import com.studyflow.project.profile.dto.ProjectProfileResponse;
import com.studyflow.project.profile.dto.ProjectTechStackRequest;
import com.studyflow.project.profile.dto.ProjectTechStackResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectHubController {
    private final ProjectHubService projectHubService;

    public ProjectHubController(ProjectHubService projectHubService) {
        this.projectHubService = projectHubService;
    }

    @GetMapping("/profile")
    public ApiResponse<ProjectProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectHubService.getProfile(principal.userId(), projectId));
    }

    @PutMapping("/profile")
    public ApiResponse<ProjectProfileResponse> upsertProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectProfileRequest request
    ) {
        return ApiResponse.success(projectHubService.upsertProfile(principal.userId(), projectId, request));
    }

    @PutMapping("/tech-stacks")
    public ApiResponse<List<ProjectTechStackResponse>> replaceTechStacks(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @RequestBody List<@Valid ProjectTechStackRequest> requests
    ) {
        return ApiResponse.success(projectHubService.replaceTechStacks(principal.userId(), projectId, requests));
    }

    @PutMapping("/portfolio")
    public ApiResponse<PortfolioProjectResponse> upsertPortfolioSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody PortfolioProjectRequest request
    ) {
        return ApiResponse.success(projectHubService.upsertPortfolioSettings(principal.userId(), projectId, request));
    }
}
