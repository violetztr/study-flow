package com.studyflow.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityPostControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircleMemberMapper circleMemberMapper;

    @Test
    void createPostReturnsPostAndFeedShowsIt() throws Exception {
        String token = registerAndLogin("post_author_alice", "post_author_alice@example.com");
        Long topicId = firstTopicId(token);

        Long postId = createPost(token, topicId, "第一条社区帖子", "今天开始把学习记录发到 Violet Circle。");

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId))
                .andExpect(jsonPath("$.data[0].title").value("第一条社区帖子"))
                .andExpect(jsonPath("$.data[0].commentCount").value(0))
                .andExpect(jsonPath("$.data[0].reactionCount").value(0));
    }

    @Test
    void anotherUserCannotUpdatePost() throws Exception {
        String aliceToken = registerAndLogin("post_owner_alice", "post_owner_alice@example.com");
        String bobToken = registerAndLogin("post_owner_bob", "post_owner_bob@example.com");
        Long topicId = firstTopicId(aliceToken);
        Long postId = createPost(aliceToken, topicId, "Alice 的帖子", "只有 Alice 能改。");

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Bob 想改",
                                  "content": "这应该失败。"
                                }
                                """.formatted(topicId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidTopicReturnsClientError() throws Exception {
        String token = registerAndLogin("post_invalid_topic_alice", "post_invalid_topic_alice@example.com");

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": 999999,
                                  "title": "Invalid topic",
                                  "content": "This topic should not exist."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void softDeletedPostDisappearsFromFeedAndDetail() throws Exception {
        String token = registerAndLogin("post_delete_alice", "post_delete_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Soon deleted", "This will be soft deleted.");

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(postId.intValue()))));

        mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulUpdateChangesTitleContentAndTopicWithoutBumpingActivity() throws Exception {
        String token = registerAndLogin("post_update_alice", "post_update_alice@example.com");
        JsonNode topics = topics(token);
        Long firstTopicId = topics.get(0).path("id").asLong();
        Long secondTopicId = topics.get(1).path("id").asLong();
        String secondTopicName = topics.get(1).path("name").asText();
        Long postId = createPost(token, firstTopicId, "Original title", "Original content.");
        JsonNode before = getPost(token, postId);
        String beforeLastActivityAt = before.path("lastActivityAt").asText();

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Updated title",
                                  "content": "Updated content."
                                }
                                """.formatted(secondTopicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.content").value("Updated content."))
                .andExpect(jsonPath("$.data.topicId").value(secondTopicId))
                .andExpect(jsonPath("$.data.topicName").value(secondTopicName))
                .andExpect(jsonPath("$.data.lastActivityAt").value(beforeLastActivityAt));
    }

    @Test
    void anotherUserCannotDeletePost() throws Exception {
        String aliceToken = registerAndLogin("post_delete_owner_alice", "post_delete_owner_alice@example.com");
        String bobToken = registerAndLogin("post_delete_owner_bob", "post_delete_owner_bob@example.com");
        Long topicId = firstTopicId(aliceToken);
        Long postId = createPost(aliceToken, topicId, "Alice delete protected", "Only Alice can delete.");

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void topicsAreOrderedAndPostCountUpdatesAfterCreateAndDelete() throws Exception {
        String token = registerAndLogin("post_topic_count_alice", "post_topic_count_alice@example.com");
        JsonNode initialTopics = topics(token);
        Long firstTopicId = initialTopics.get(0).path("id").asLong();
        int initialCount = initialTopics.get(0).path("postCount").asInt();

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].slug").value("learning"))
                .andExpect(jsonPath("$.data[1].slug").value("notes"))
                .andExpect(jsonPath("$.data[2].slug").value("daily"))
                .andExpect(jsonPath("$.data[3].slug").value("projects"));

        Long postId = createPost(token, firstTopicId, "Counted post", "This should increment the topic count.");
        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postCount").value(initialCount + 1));

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postCount").value(initialCount));
    }

    @Test
    void validationRejectsBlankTitleBlankContentAndOverlongContent() throws Exception {
        String token = registerAndLogin("post_validation_alice", "post_validation_alice@example.com");
        Long topicId = firstTopicId(token);

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": " ",
                                  "content": "Valid content."
                                }
                                """.formatted(topicId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Valid title",
                                  "content": " "
                                }
                                """.formatted(topicId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("topicId", topicId)
                                .put("title", "Valid title")
                                .put("content", "a".repeat(10001))
                                .toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void readRoutesRequireDefaultCircleMembership() throws Exception {
        String memberToken = registerAndLogin("post_read_member", "post_read_member@example.com");
        Long topicId = firstTopicId(memberToken);
        Long postId = createPost(memberToken, topicId, "Members only", "Circle visibility requires membership.");
        String nonMemberToken = registerAndLogin("post_read_non_member", "post_read_non_member@example.com");
        removeDefaultMembership(extractUserId(nonMemberToken));

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + nonMemberToken))
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
        return topics(token).get(0).path("id").asLong();
    }

    private JsonNode topics(String token) throws Exception {
        MvcResult topicsResult = mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(topicsResult.getResponse().getContentAsByteArray());
        return response.path("data");
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

    private JsonNode getPost(String token, Long postId) throws Exception {
        MvcResult postResult = mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data");
    }

    private Long extractUserId(String token) throws Exception {
        String[] parts = token.split("\\.");
        JsonNode payload = objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(parts[1]));
        return payload.path("userId").asLong();
    }

    private void removeDefaultMembership(Long userId) {
        circleMemberMapper.delete(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getUserId, userId));
    }
}
