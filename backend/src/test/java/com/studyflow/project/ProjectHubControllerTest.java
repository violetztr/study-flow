package com.studyflow.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectHubControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void upsertProjectProfileStoresEngineeringMetadata() throws Exception {
        String token = registerAndLogin("hub_profile_user", "hub_profile_user@example.com");
        Long projectId = createProject(token, "DevFlow Studio");

        mockMvc.perform(put("/api/projects/{id}/profile", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline": "个人全栈研发中台",
                                  "productionUrl": "https://www.violet-surf.com",
                                  "apiDocUrl": "https://www.violet-surf.com/doc.html",
                                  "databaseDocUrl": "https://github.com/violetztr/study-flow/blob/main/docs/database.md",
                                  "architectureSummary": "React + Spring Boot + MySQL + Docker",
                                  "interviewHighlights": "GitHub 集成、公开作品集、Docker 部署",
                                  "coverImageUrl": "https://example.com/cover.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.headline").value("个人全栈研发中台"))
                .andExpect(jsonPath("$.data.productionUrl").value("https://www.violet-surf.com"));

        mockMvc.perform(get("/api/projects/{id}/profile", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.architectureSummary").value("React + Spring Boot + MySQL + Docker"));
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
                                  "description": "测试项目",
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
