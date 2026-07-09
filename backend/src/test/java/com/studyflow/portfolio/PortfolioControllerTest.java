package com.studyflow.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PortfolioControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPortfolioReturnsVisibleProjectsWithoutLogin() throws Exception {
        String token = registerAndLogin("portfolio_user", "portfolio_user@example.com");
        Long projectId = createProject(token, "DevFlow Studio");
        upsertProfile(token, projectId, "Personal full-stack engineering command center");
        replaceTechStacks(token, projectId);
        publishPortfolio(token, projectId, "devflow-studio-public");

        mockMvc.perform(get("/api/portfolio/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].slug").value("devflow-studio-public"))
                .andExpect(jsonPath("$.data[0].techStacks", hasSize(3)));
    }

    @Test
    void publicPortfolioDetailReturnsProjectBySlug() throws Exception {
        String token = registerAndLogin("portfolio_detail_user", "portfolio_detail_user@example.com");
        Long projectId = createProject(token, "DevFlow Studio");
        upsertProfile(token, projectId, "Personal full-stack engineering command center");
        publishPortfolio(token, projectId, "devflow-studio-detail");

        mockMvc.perform(get("/api/portfolio/projects/devflow-studio-detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("DevFlow Studio"))
                .andExpect(jsonPath("$.data.headline").value("Personal full-stack engineering command center"));
    }

    private void upsertProfile(String token, Long projectId, String headline) throws Exception {
        mockMvc.perform(put("/api/projects/{id}/profile", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline": "%s",
                                  "productionUrl": "https://www.violet-surf.com",
                                  "apiDocUrl": "https://www.violet-surf.com/doc.html",
                                  "databaseDocUrl": "https://github.com/violetztr/study-flow/blob/main/docs/database.md",
                                  "architectureSummary": "React + Spring Boot + MySQL + Docker",
                                  "interviewHighlights": "Owns database design, tests, deployment, and public portfolio.",
                                  "coverImageUrl": "https://example.com/cover.png"
                                }
                                """.formatted(headline)))
                .andExpect(status().isOk());
    }

    private void replaceTechStacks(String token, Long projectId) throws Exception {
        mockMvc.perform(put("/api/projects/{id}/tech-stacks", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "name": "React", "category": "FRONTEND", "sortOrder": 1 },
                                  { "name": "Spring Boot", "category": "BACKEND", "sortOrder": 2 },
                                  { "name": "Docker", "category": "DEPLOYMENT", "sortOrder": 3 }
                                ]
                                """))
                .andExpect(status().isOk());
    }

    private void publishPortfolio(String token, Long projectId, String slug) throws Exception {
        mockMvc.perform(put("/api/projects/{id}/portfolio", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "%s",
                                  "publicVisible": true,
                                  "featured": true,
                                  "displayOrder": 1,
                                  "publicSummary": "A practical full-stack project for learning and interviews."
                                }
                                """.formatted(slug)))
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
                                  "description": "Portfolio test project",
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
