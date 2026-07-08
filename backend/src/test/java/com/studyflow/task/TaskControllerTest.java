package com.studyflow.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createTaskCanBindTags() throws Exception {
        String token = registerAndLogin("task_user_alice", "task_user_alice@example.com");
        Long projectId = createProject(token, "任务测试项目");
        Long tagId = createTag(token, "后端");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": %d,
                                  "title": "实现任务接口",
                                  "description": "完成任务新增接口",
                                  "status": "IN_PROGRESS",
                                  "priority": "HIGH",
                                  "estimatedMinutes": 90,
                                  "tagIds": [%d]
                                }
                                """.formatted(projectId, tagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("实现任务接口"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.estimatedMinutes").value(90))
                .andExpect(jsonPath("$.data.tagIds[0]").value(tagId));
    }

    @Test
    void createTaskRejectsNegativeEstimatedMinutes() throws Exception {
        String token = registerAndLogin("task_user_duration", "task_user_duration@example.com");
        Long projectId = createProject(token, "时长校验项目");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": %d,
                                  "title": "错误时长任务",
                                  "description": "预计学习时长不能为负数",
                                  "status": "PENDING",
                                  "priority": "MEDIUM",
                                  "estimatedMinutes": -1,
                                  "tagIds": []
                                }
                                """.formatted(projectId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("estimatedMinutes: 预计学习时长不能小于 0"));
    }

    @Test
    void listTasksCanFilterByProjectAndStatus() throws Exception {
        String token = registerAndLogin("task_user_bob", "task_user_bob@example.com");
        Long projectId = createProject(token, "筛选项目 A");
        Long otherProjectId = createProject(token, "筛选项目 B");

        createTask(token, projectId, "待开始任务", "PENDING", "MEDIUM");
        createTask(token, projectId, "已完成任务", "DONE", "LOW");
        createTask(token, otherProjectId, "其他项目任务", "DONE", "HIGH");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .param("projectId", projectId.toString())
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("已完成任务"));
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

    private Long createTag(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "color": "#1677ff"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();

        return extractId(result);
    }

    private void createTask(String token, Long projectId, String title, String status, String priority) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": %d,
                                  "title": "%s",
                                  "description": "测试任务",
                                  "status": "%s",
                                  "priority": "%s",
                                  "tagIds": []
                                }
                                """.formatted(projectId, title, status, priority)))
                .andExpect(status().isOk());
    }

    private Long extractId(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.data.id");
        return id.longValue();
    }
}
