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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFavoriteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void favoritePostIsIdempotentAndAppearsInMyFavorites() throws Exception {
        String token = registerAndLogin("favorite_alice", "favorite_alice@example.com");
        Long postId = createPost(token, firstTopicId(token), "收藏测试", "收藏后可以在我的收藏里看到。");

        favoritePost(token, postId);
        favoritePost(token, postId);

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favoriteCount").value(1))
                .andExpect(jsonPath("$.data.favoritedByCurrentUser").value(true));

        mockMvc.perform(get("/api/community/favorites/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId))
                .andExpect(jsonPath("$.data[0].favoriteCount").value(1))
                .andExpect(jsonPath("$.data[0].favoritedByCurrentUser").value(true));
    }

    @Test
    void unfavoritePostUpdatesCountAndRemovesFromMyFavorites() throws Exception {
        String token = registerAndLogin("favorite_bob", "favorite_bob@example.com");
        Long postId = createPost(token, firstTopicId(token), "取消收藏测试", "取消后不再出现在我的收藏。");
        favoritePost(token, postId);

        mockMvc.perform(delete("/api/community/posts/{postId}/favorites", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favoriteCount").value(0))
                .andExpect(jsonPath("$.data.favoritedByCurrentUser").value(false));

        mockMvc.perform(get("/api/community/favorites/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void favoriteRoutesRequireLogin() throws Exception {
        String token = registerAndLogin("favorite_auth_owner", "favorite_auth_owner@example.com");
        Long postId = createPost(token, firstTopicId(token), "收藏登录限制", "游客不能收藏。");

        mockMvc.perform(post("/api/community/posts/{postId}/favorites", postId))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/community/posts/{postId}/favorites", postId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/community/favorites/my"))
                .andExpect(status().isForbidden());
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

    private void favoritePost(String token, Long postId) throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/favorites", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
