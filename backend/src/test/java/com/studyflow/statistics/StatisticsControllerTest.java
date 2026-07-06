package com.studyflow.statistics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void overviewReturnsCurrentUsersTaskCounts() throws Exception {
        String token = registerAndLogin("stats_user_alice", "stats_user_alice@example.com");
        String otherToken = registerAndLogin("stats_user_bob", "stats_user_bob@example.com");
        Long projectId = createProject(token, "统计项目");
        Long otherProjectId = createProject(otherToken, "其他用户项目");

        createTask(token, projectId, "待开始任务", "PENDING", "2027-01-01T00:00:00");
        createTask(token, projectId, "进行中任务", "IN_PROGRESS", "2027-01-01T00:00:00");
        createTask(token, projectId, "已完成任务", "DONE", "2020-01-01T00:00:00");
        createTask(token, projectId, "逾期任务", "PENDING", "2020-01-01T00:00:00");
        createTask(otherToken, otherProjectId, "其他用户任务", "DONE", "2020-01-01T00:00:00");

        mockMvc.perform(get("/api/statistics/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalTasks").value(4))
                .andExpect(jsonPath("$.data.completedTasks").value(1))
                .andExpect(jsonPath("$.data.inProgressTasks").value(1))
                .andExpect(jsonPath("$.data.overdueTasks").value(1));
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

        return extractId(result);
    }

    private void createTask(String token, Long projectId, String title, String status, String deadline) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": %d,
                                  "title": "%s",
                                  "description": "统计测试任务",
                                  "status": "%s",
                                  "priority": "MEDIUM",
                                  "deadline": "%s",
                                  "tagIds": []
                                }
                                """.formatted(projectId, title, status, deadline)))
                .andExpect(status().isOk());
    }

    private Long extractId(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.data.id");
        return id.longValue();
    }
}
