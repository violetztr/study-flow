package com.studyflow.github.dto;

import com.studyflow.github.GitHubRepository;

import java.time.LocalDateTime;

public record GitHubRepositoryResponse(
        Long id,
        Long projectId,
        String owner,
        String repo,
        String htmlUrl,
        String description,
        String defaultBranch,
        String primaryLanguage,
        Integer stars,
        Integer forks,
        Integer openIssues,
        LocalDateTime pushedAt,
        LocalDateTime lastSyncedAt,
        Boolean readmePresent,
        String languagesJson,
        String latestCommitsJson
) {
    public static GitHubRepositoryResponse from(GitHubRepository repository) {
        return new GitHubRepositoryResponse(
                repository.getId(),
                repository.getProjectId(),
                repository.getOwner(),
                repository.getRepo(),
                repository.getHtmlUrl(),
                repository.getDescription(),
                repository.getDefaultBranch(),
                repository.getPrimaryLanguage(),
                repository.getStars(),
                repository.getForks(),
                repository.getOpenIssues(),
                repository.getPushedAt(),
                repository.getLastSyncedAt(),
                repository.getReadmePresent(),
                repository.getLanguagesJson(),
                repository.getLatestCommitsJson()
        );
    }
}
