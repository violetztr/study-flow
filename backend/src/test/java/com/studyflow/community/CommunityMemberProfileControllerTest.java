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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityMemberProfileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicProfileShowsPublishedWorksWithoutLogin() throws Exception {
        String authorToken = registerAndLogin("profile_public_author", "profile_public_author@example.com");
        Long authorId = extractUserId(authorToken);

        updateProfile(authorToken, "Ruru Author", "把作品放在主页里展示。");
        createPost(authorToken, "公开作品 A", "游客也能看到作者主页里的作品。");
        createPost(authorToken, "公开作品 B", "主页按照发布时间展示作品。");

        mockMvc.perform(get("/api/community/profiles/{userId}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.userId").value(authorId))
                .andExpect(jsonPath("$.data.member.displayName").value("Ruru Author"))
                .andExpect(jsonPath("$.data.member.followedByCurrentUser").value(false))
                .andExpect(jsonPath("$.data.currentUserProfile").value(false))
                .andExpect(jsonPath("$.data.totalPostCount").value(2))
                .andExpect(jsonPath("$.data.articleCount").value(2))
                .andExpect(jsonPath("$.data.videoCount").value(0))
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.favoritePosts.length()").value(0));
    }

    @Test
    void loggedInViewerSeesFollowStateOnProfile() throws Exception {
        String authorToken = registerAndLogin("profile_follow_author", "profile_follow_author@example.com");
        Long authorId = extractUserId(authorToken);
        String viewerToken = registerAndLogin("profile_follow_viewer", "profile_follow_viewer@example.com");

        mockMvc.perform(post("/api/community/members/{userId}/follow", authorId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/profiles/{userId}", authorId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.followerCount").value(1))
                .andExpect(jsonPath("$.data.member.followedByCurrentUser").value(true));
    }

    @Test
    void currentUserProfileIncludesOwnFavorites() throws Exception {
        String token = registerAndLogin("profile_favorite_owner", "profile_favorite_owner@example.com");
        Long userId = extractUserId(token);
        Long postId = createPost(token, "收藏会出现在自己的主页", "自己的主页可以放一个收藏区。");

        mockMvc.perform(post("/api/community/posts/{postId}/favorites", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/profiles/{userId}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUserProfile").value(true))
                .andExpect(jsonPath("$.data.favoritePosts[0].id").value(postId))
                .andExpect(jsonPath("$.data.favoritePosts[0].favoritedByCurrentUser").value(true));
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

    private void updateProfile(String token, String displayName, String bio) throws Exception {
        mockMvc.perform(put("/api/community/members/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "bio": "%s"
                                }
                                """.formatted(displayName, bio)))
                .andExpect(status().isOk());
    }

    private Long createPost(String token, String title, String content) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "content": "%s",
                                  "topicName": "日常"
                                }
                                """.formatted(title, content)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private Long extractUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/community/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.path("data").path("userId").asLong();
    }
}
