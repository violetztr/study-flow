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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileDisplayControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void profileAvatarAndDisplayNamePropagateToPostsAndComments() throws Exception {
        String token = registerAndLogin("profile_display_alice", "profile_display_alice@example.com");
        String displayName = "Alice Display";
        String avatarUrl = "https://cdn.example.com/alice-avatar.png";

        mockMvc.perform(put("/api/community/members/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "avatarUrl": "%s"
                                }
                                """.formatted(displayName, avatarUrl)))
                .andExpect(status().isOk());

        Long topicId = firstTopicId(token);
        MvcResult createdPostResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Profile display post",
                                  "content": "Author display should follow latest profile."
                                }
                                """.formatted(topicId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode createdPost = data(createdPostResult);
        Long postId = createdPost.path("id").asLong();
        assertPostAuthor(createdPost, displayName, avatarUrl);

        MvcResult postDetailResult = mockMvc.perform(get("/api/community/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andReturn();
        assertPostAuthor(data(postDetailResult), displayName, avatarUrl);

        MvcResult feedResult = mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode feedPost = findArrayItemById(data(feedResult), postId);
        assertPostAuthor(feedPost, displayName, avatarUrl);

        MvcResult createdCommentResult = mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Comment avatar should also be fresh."
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        assertCommentAuthor(data(createdCommentResult), displayName, avatarUrl);

        MvcResult commentsResult = mockMvc.perform(get("/api/community/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode comment = findArrayItemByAuthorName(data(commentsResult), displayName);
        assertCommentAuthor(comment, displayName, avatarUrl);
    }

    private void assertPostAuthor(JsonNode post, String displayName, String avatarUrl) {
        assertThat(post.path("authorName").asText()).isEqualTo(displayName);
        assertThat(post.path("authorAvatarUrl").asText()).isEqualTo(avatarUrl);
    }

    private void assertCommentAuthor(JsonNode comment, String displayName, String avatarUrl) {
        assertThat(comment.path("authorName").asText()).isEqualTo(displayName);
        assertThat(comment.path("authorAvatarUrl").asText()).isEqualTo(avatarUrl);
    }

    private JsonNode findArrayItemById(JsonNode array, Long id) {
        for (JsonNode item : array) {
            if (item.path("id").asLong() == id) {
                return item;
            }
        }
        throw new AssertionError("Could not find item with id " + id);
    }

    private JsonNode findArrayItemByAuthorName(JsonNode array, String authorName) {
        for (JsonNode item : array) {
            if (authorName.equals(item.path("authorName").asText())) {
                return item;
            }
        }
        throw new AssertionError("Could not find item by author " + authorName);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
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
}
