package com.studyflow.project.profile;

import com.studyflow.common.ApiResponse;
import com.studyflow.project.profile.dto.ProjectProfileRequest;
import com.studyflow.project.profile.dto.ProjectProfileResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
