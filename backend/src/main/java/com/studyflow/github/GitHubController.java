package com.studyflow.github;

import com.studyflow.common.ApiResponse;
import com.studyflow.github.dto.GitHubRepositoryRequest;
import com.studyflow.github.dto.GitHubRepositoryResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/github")
public class GitHubController {
    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @PutMapping
    public ApiResponse<GitHubRepositoryResponse> upsertRepository(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody GitHubRepositoryRequest request
    ) {
        return ApiResponse.success(gitHubService.upsertRepository(principal.userId(), projectId, request));
    }

    @PostMapping("/sync")
    public ApiResponse<GitHubRepositoryResponse> syncRepository(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(gitHubService.syncRepository(principal.userId(), projectId));
    }
}
