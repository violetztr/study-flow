package com.studyflow.github.dto;

import java.time.LocalDateTime;

public record GitHubSyncResult(
        String htmlUrl,
        String description,
        String defaultBranch,
        String primaryLanguage,
        Integer stars,
        Integer forks,
        Integer openIssues,
        LocalDateTime pushedAt,
        Boolean readmePresent,
        String languagesJson,
        String latestCommitsJson
) {
}
