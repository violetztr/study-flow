package com.studyflow.github;

import com.studyflow.github.dto.GitHubSyncResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class FakeGitHubConfig {
        @Bean
        @Primary
        GitHubClient gitHubClient() {
            return (owner, repo) -> new GitHubSyncResult(
                    "https://github.com/violetztr/study-flow",
                    "DevFlow Studio repository",
                    "main",
                    "Java",
                    12,
                    3,
                    1,
                    LocalDateTime.parse("2026-07-09T12:00:00"),
                    true,
                    "{\"Java\":1000,\"TypeScript\":800}",
                    "[{\"sha\":\"abc123\",\"message\":\"feat: test\"}]"
            );
        }
    }

    @Test
    void upsertGitHubRepositoryStoresOwnerAndRepo() throws Exception {
        String token = registerAndLogin("github_user", "github_user@example.com");
        Long projectId = createProject(token, "DevFlow Studio");

        mockMvc.perform(put("/api/projects/{id}/github", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "violetztr",
                                  "repo": "study-flow"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value("violetztr"))
                .andExpect(jsonPath("$.data.repo").value("study-flow"));
    }

    @Test
    void syncGitHubRepositoryUpdatesMetadata() throws Exception {
        String token = registerAndLogin("github_sync_user", "github_sync_user@example.com");
        Long projectId = createProject(token, "DevFlow Studio");
        upsertGitHubRepository(token, projectId, "violetztr", "study-flow");

        mockMvc.perform(post("/api/projects/{id}/github/sync", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.htmlUrl").value("https://github.com/violetztr/study-flow"))
                .andExpect(jsonPath("$.data.primaryLanguage").value("Java"))
                .andExpect(jsonPath("$.data.readmePresent").value(true));
    }

    private void upsertGitHubRepository(
            String token,
            Long projectId,
            String owner,
            String repo
    ) throws Exception {
        mockMvc.perform(put("/api/projects/{id}/github", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "%s",
                                  "repo": "%s"
                                }
                                """.formatted(owner, repo)))
                .andExpect(status().isOk());
    }

    private String registerAndLogin(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username, email)))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
                .andReturn();

        return loginResult.getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private Long createProject(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Test project",
                                  "status": "ACTIVE"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");
        return Long.valueOf(id);
    }
}
