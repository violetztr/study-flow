package com.studyflow.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostMapper;
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
class CommunitySocialControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Test
    void loginGrantsOnePigCoinOnlyOncePerDay() throws Exception {
        register("pig_login_alice", "pig_login_alice@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pig_login_alice",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wallet.pigBalance").value(1))
                .andExpect(jsonPath("$.data.wallet.todayGranted").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pig_login_alice",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wallet.pigBalance").value(1))
                .andExpect(jsonPath("$.data.wallet.todayGranted").value(false));
    }

    @Test
    void usersCanFollowAndUnfollowPostAuthors() throws Exception {
        String authorToken = registerAndLogin("follow_author", "follow_author@example.com");
        String followerToken = registerAndLogin("follow_follower", "follow_follower@example.com");
        Long authorId = extractUserId(authorToken);

        mockMvc.perform(post("/api/community/members/{userId}/follow", authorId)
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.followerCount").value(1));

        mockMvc.perform(get("/api/community/members/{userId}", authorId)
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.followerCount").value(1));

        mockMvc.perform(delete("/api/community/members/{userId}/follow", authorId)
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(false))
                .andExpect(jsonPath("$.data.followerCount").value(0));
    }

    @Test
    void usersCannotFollowThemselves() throws Exception {
        String token = registerAndLogin("follow_self", "follow_self@example.com");
        Long userId = extractUserId(token);

        mockMvc.perform(post("/api/community/members/{userId}/follow", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void danmakuCanBeReadPubliclyButSendingRequiresLogin() throws Exception {
        String token = registerAndLogin("danmaku_author", "danmaku_author@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Video post", "A video post can receive danmaku.");
        markAsPublishedVideo(postId);

        mockMvc.perform(get("/api/community/posts/{postId}/danmaku", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(post("/api/community/posts/{postId}/danmaku", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "guest cannot send",
                                  "timeSeconds": 1
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/community/posts/{postId}/danmaku", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "first danmaku",
                                  "timeSeconds": 3,
                                  "color": "#ffffff"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("first danmaku"))
                .andExpect(jsonPath("$.data.timeSeconds").value(3));

        mockMvc.perform(get("/api/community/posts/{postId}/danmaku", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("first danmaku"))
                .andExpect(jsonPath("$.data[0].authorName").value("danmaku_author"));
    }

    @Test
    void danmakuBelongsToVideoPostsAndKeepsTimelineOrderAndColor() throws Exception {
        String token = registerAndLogin("danmaku_timeline", "danmaku_timeline@example.com");
        Long topicId = firstTopicId(token);
        Long articlePostId = createPost(token, topicId, "Article post", "Articles should not receive danmaku.");

        mockMvc.perform(post("/api/community/posts/{postId}/danmaku", articlePostId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "article danmaku",
                                  "timeSeconds": 1,
                                  "color": "#ffffff"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        Long videoPostId = createPost(token, topicId, "Timeline video", "Video danmaku should be timeline data.");
        markAsPublishedVideo(videoPostId);

        mockMvc.perform(post("/api/community/posts/{postId}/danmaku", videoPostId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "late danmaku",
                                  "timeSeconds": 18,
                                  "color": "#66ccff"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/posts/{postId}/danmaku", videoPostId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "early danmaku",
                                  "timeSeconds": 3,
                                  "color": "#ff6699"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}/danmaku", videoPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("early danmaku"))
                .andExpect(jsonPath("$.data[0].timeSeconds").value(3))
                .andExpect(jsonPath("$.data[0].color").value("#ff6699"))
                .andExpect(jsonPath("$.data[1].content").value("late danmaku"))
                .andExpect(jsonPath("$.data[1].timeSeconds").value(18))
                .andExpect(jsonPath("$.data[1].color").value("#66ccff"));

        mockMvc.perform(get("/api/community/posts/{postId}", videoPostId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.danmakuCount").value(2));
    }

    private void register(String username, String email) throws Exception {
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
    }

    private String registerAndLogin(String username, String email) throws Exception {
        register(username, email);

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

    private Long extractUserId(String token) throws Exception {
        String[] parts = token.split("\\.");
        JsonNode payload = objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(parts[1]));
        return payload.path("userId").asLong();
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

    private void markAsPublishedVideo(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        post.setContentType("VIDEO");
        post.setStatus("PUBLISHED");
        communityPostMapper.updateById(post);
    }
}
