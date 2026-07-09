package com.studyflow.daily;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DailyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndListPlansByDate() throws Exception {
        String token = registerAndLogin("daily_plan_user", "daily_plan_user@example.com");

        mockMvc.perform(post("/api/daily/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planDate": "2026-07-09",
                                  "title": "完成笔记模块后端",
                                  "description": "写测试、建表、写接口",
                                  "status": "TODO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("完成笔记模块后端"))
                .andExpect(jsonPath("$.data.status").value("TODO"));

        mockMvc.perform(get("/api/daily/plans?date=2026-07-09")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("完成笔记模块后端"));
    }

    @Test
    void updatePlanStatus() throws Exception {
        String token = registerAndLogin("daily_status_user", "daily_status_user@example.com");
        Long planId = createPlan(token, "2026-07-09", "推进日常模块");

        mockMvc.perform(put("/api/daily/plans/{id}", planId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planDate": "2026-07-09",
                                  "title": "推进日常模块",
                                  "description": "更新为完成",
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    @Test
    void upsertJournalByDate() throws Exception {
        String token = registerAndLogin("daily_journal_user", "daily_journal_user@example.com");

        mockMvc.perform(put("/api/daily/journal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "journalDate": "2026-07-09",
                                  "mood": "FOCUSED",
                                  "content": "今天把后端数据库补齐。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mood").value("FOCUSED"));

        mockMvc.perform(put("/api/daily/journal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "journalDate": "2026-07-09",
                                  "mood": "HAPPY",
                                  "content": "第二次保存应该更新同一天日记。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mood").value("HAPPY"))
                .andExpect(jsonPath("$.data.content").value("第二次保存应该更新同一天日记。"));
    }

    @Test
    void createHabitAndRecordToday() throws Exception {
        String token = registerAndLogin("daily_habit_user", "daily_habit_user@example.com");

        MvcResult habitResult = mockMvc.perform(post("/api/daily/habits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "每天写项目",
                                  "description": "保持全栈项目推进"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("每天写项目"))
                .andReturn();

        Long habitId = extractId(habitResult);

        mockMvc.perform(put("/api/daily/habits/{id}/records", habitId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-07-09",
                                  "completed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));
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

    private Long createPlan(String token, String date, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/daily/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planDate": "%s",
                                  "title": "%s",
                                  "status": "TODO"
                                }
                                """.formatted(date, title)))
                .andExpect(status().isOk())
                .andReturn();
        return extractId(result);
    }

    private Long extractId(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\\\"id\\\":(\\d+)").matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException("响应中没有 id: " + response);
        }
        return Long.valueOf(matcher.group(1));
    }
}
