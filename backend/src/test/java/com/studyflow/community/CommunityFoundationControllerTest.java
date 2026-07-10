package com.studyflow.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFoundationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerCreatesDefaultCommunityMembership() throws Exception {
        String token = registerAndLogin("circle_foundation_alice", "circle_foundation_alice@example.com");

        mockMvc.perform(get("/api/community/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("circle_foundation_alice"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.circleSlug").value("violet-circle"));
    }

    @Test
    void updateProfileChangesCurrentMemberDisplayName() throws Exception {
        String token = registerAndLogin("circle_profile_alice", "circle_profile_alice@example.com");

        mockMvc.perform(put("/api/community/members/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Alice Circle",
                                  "bio": "Learning full stack step by step",
                                  "skills": "Java,React,Docker",
                                  "githubUrl": "https://github.com/alice",
                                  "websiteUrl": "https://example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Alice Circle"))
                .andExpect(jsonPath("$.data.bio").value("Learning full stack step by step"));
    }

    @Test
    void listMembersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/community/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMembersReturnsDefaultCircleMembers() throws Exception {
        String aliceToken = registerAndLogin("circle_list_alice", "circle_list_alice@example.com");
        registerAndLogin("circle_list_bob", "circle_list_bob@example.com");

        mockMvc.perform(put("/api/community/members/me/profile")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Alice List",
                                  "bio": "Community builder",
                                  "skills": "Java,React"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/members")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.username == 'circle_list_alice')].displayName").value("Alice List"))
                .andExpect(jsonPath("$.data[?(@.username == 'circle_list_alice')].bio").value("Community builder"))
                .andExpect(jsonPath("$.data[?(@.username == 'circle_list_alice')].skills").value("Java,React"))
                .andExpect(jsonPath("$.data[?(@.username == 'circle_list_alice')].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[?(@.username == 'circle_list_alice')].memberStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data[?(@.username == 'circle_list_bob')].username").value("circle_list_bob"));
    }

    @ParameterizedTest
    @CsvSource({
            "displayName,81",
            "bio,501",
            "avatarUrl,501",
            "skills,501",
            "githubUrl,301",
            "websiteUrl,301"
    })
    void updateProfileRejectsOverlongValues(String fieldName, int length) throws Exception {
        String username = "circle_validation_" + fieldName;
        String token = registerAndLogin(username, username + "@example.com");
        String overlongValue = "a".repeat(length);

        mockMvc.perform(put("/api/community/members/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "%s": "%s"
                                }
                                """.formatted(fieldName, overlongValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getMemberReturnsNotFoundWhenTargetIsNotCommunityMember() throws Exception {
        String token = registerAndLogin("circle_lookup_alice", "circle_lookup_alice@example.com");

        mockMvc.perform(get("/api/community/members/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
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
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }
}
