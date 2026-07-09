package com.studyflow.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityReactionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void likePostIsIdempotentAndUpdatesCount() throws Exception {
        String token = registerAndLogin("reaction_alice", "reaction_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "点赞测试", "重复点赞只能算一次。");

        likePost(token, postId);
        likePost(token, postId);

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reactionCount").value(1))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    @Test
    void unlikePostUpdatesCount() throws Exception {
        String token = registerAndLogin("reaction_bob", "reaction_bob@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "取消点赞测试", "取消后计数为 0。");

        likePost(token, postId);

        mockMvc.perform(delete("/api/community/posts/{postId}/reactions/like", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reactionCount").value(0))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(false));
    }

    @Test
    void unlikeWithoutPriorLikeIsOkAndKeepsCountAtZero() throws Exception {
        String token = registerAndLogin("reaction_unlike_alice", "reaction_unlike_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "未点赞取消", "没有点赞时取消也应成功。");

        mockMvc.perform(delete("/api/community/posts/{postId}/reactions/like", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reactionCount").value(0))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(false));
    }

    @Test
    void differentUsersLikingSamePostIncrementCount() throws Exception {
        String aliceToken = registerAndLogin("reaction_multi_alice", "reaction_multi_alice@example.com");
        String bobToken = registerAndLogin("reaction_multi_bob", "reaction_multi_bob@example.com");
        Long topicId = firstTopicId(aliceToken);
        Long postId = createPost(aliceToken, topicId, "多人点赞", "不同用户点赞分别计数。");

        likePost(aliceToken, postId);
        likePost(bobToken, postId);

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reactionCount").value(2))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    @Test
    void likingDeletedPostFails() throws Exception {
        String token = registerAndLogin("reaction_deleted_alice", "reaction_deleted_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "删除后点赞", "删除后不能点赞。");

        mockMvc.perform(delete("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/posts/{postId}/reactions/like", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
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

        JsonNode response = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        return response.path("data").path("token").asText();
    }

    private Long firstTopicId(String token) throws Exception {
        MvcResult topicsResult = mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(topicsResult.getResponse().getContentAsByteArray());
        return response.path("data").get(0).path("id").asLong();
    }

    private Long createPost(String token, Long topicId, String title, String content) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "%s",
                                  "content": "%s"
                                }
                                """.formatted(topicId, title, content)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private void likePost(String token, Long postId) throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/reactions/like", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
