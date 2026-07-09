package com.studyflow.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.common.BusinessException;
import com.studyflow.github.dto.GitHubSyncResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class GitHubApiClient implements GitHubClient {
    private static final String API_BASE_URL = "https://api.github.com/repos";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GitHubApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public GitHubSyncResult fetchRepository(String owner, String repo) {
        String repositoryPath = encode(owner) + "/" + encode(repo);
        JsonNode repository = getJson(API_BASE_URL + "/" + repositoryPath);
        String languagesJson = getBody(API_BASE_URL + "/" + repositoryPath + "/languages");
        String latestCommitsJson = getBody(API_BASE_URL + "/" + repositoryPath + "/commits?per_page=5");
        boolean readmePresent = exists(API_BASE_URL + "/" + repositoryPath + "/readme");

        return new GitHubSyncResult(
                text(repository, "html_url"),
                text(repository, "description"),
                text(repository, "default_branch"),
                text(repository, "language"),
                repository.path("stargazers_count").asInt(0),
                repository.path("forks_count").asInt(0),
                repository.path("open_issues_count").asInt(0),
                parseGitHubTime(text(repository, "pushed_at")),
                readmePresent,
                languagesJson,
                latestCommitsJson
        );
    }

    private JsonNode getJson(String url) {
        try {
            return objectMapper.readTree(getBody(url));
        } catch (IOException ex) {
            throw new BusinessException(400, "GitHub response cannot be parsed");
        }
    }

    private String getBody(String url) {
        HttpResponse<String> response = send(url);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(400, "GitHub repository fetch failed");
        }
        return response.body();
    }

    private boolean exists(String url) {
        HttpResponse<String> response = send(url);
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private HttpResponse<String> send(String url) {
        try {
            return httpClient.send(buildRequest(url), HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new BusinessException(400, "GitHub API is unavailable");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(400, "GitHub API request was interrupted");
        }
    }

    private HttpRequest buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "StudyFlow-DevFlow");

        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        return builder.build();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private LocalDateTime parseGitHubTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toLocalDateTime();
    }
}
