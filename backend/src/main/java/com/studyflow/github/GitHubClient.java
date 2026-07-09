package com.studyflow.github;

import com.studyflow.github.dto.GitHubSyncResult;

public interface GitHubClient {
    GitHubSyncResult fetchRepository(String owner, String repo);
}
