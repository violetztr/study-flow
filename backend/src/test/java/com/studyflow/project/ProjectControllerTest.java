package com.studyflow.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createProjectReturnsProjectData() throws Exception {
        String token = registerAndLogin("project_alice", "project_alice@example.com");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java 全栈学习",
                                  "description": "学习 Spring Boot 和 React",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Java 全栈学习"))
                .andExpect(jsonPath("$.data.description").value("学习 Spring Boot 和 React"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void listProjectsOnlyReturnsCurrentUsersProjects() throws Exception {
        String aliceToken = registerAndLogin("project_owner_alice", "project_owner_alice@example.com");
        String bobToken = registerAndLogin("project_owner_bob", "project_owner_bob@example.com");

        createProject(aliceToken, "Alice 的项目");
        createProject(bobToken, "Bob 的项目");

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Alice 的项目"));
    }

    @Test
    void updateProjectChangesProjectData() throws Exception {
        String token = registerAndLogin("project_update_user", "project_update_user@example.com");
        Long projectId = createProject(token, "旧项目名");

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "新项目名",
                                  "description": "已经更新",
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("新项目名"))
                .andExpect(jsonPath("$.data.description").value("已经更新"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    void deleteProjectRemovesProjectFromList() throws Exception {
        String token = registerAndLogin("project_delete_user", "project_delete_user@example.com");
        Long projectId = createProject(token, "准备删除的项目");

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
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
