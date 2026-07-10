package com.studyflow.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.comment.CommunityCommentMapper;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.community.post.CommunityPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityCommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private CommunityCommentMapper communityCommentMapper;

    @Autowired
    private CircleMemberMapper circleMemberMapper;

    @Autowired
    private CommunityPostService communityPostService;

    @Test
    void addCommentIncrementsPostCommentCount() throws Exception {
        String token = registerAndLogin("comment_alice", "comment_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "评论测试", "这条帖子会收到评论。");

        mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "第一条评论"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("第一条评论"));

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentCount").value(1));
    }

    @Test
    void anotherUserCannotDeleteComment() throws Exception {
        String aliceToken = registerAndLogin("comment_owner_alice", "comment_owner_alice@example.com");
        String bobToken = registerAndLogin("comment_owner_bob", "comment_owner_bob@example.com");
        Long topicId = firstTopicId(aliceToken);
        Long postId = createPost(aliceToken, topicId, "评论权限", "Bob 不能删 Alice 的评论。");
        Long commentId = createComment(aliceToken, postId, "Alice 的评论");

        mockMvc.perform(delete("/api/community/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutedOwnerCannotDeleteComment() throws Exception {
        String token = registerAndLogin("comment_muted_delete_owner", "comment_muted_delete_owner@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Muted comment delete", "Muted users should keep read-only access.");
        Long commentId = createComment(token, postId, "Cannot delete after mute.");
        setDefaultMembershipStatus(extractUserId(token), "MUTED");

        mockMvc.perform(delete("/api/community/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(communityCommentMapper.selectById(commentId).getStatus()).isEqualTo("PUBLISHED");
        assertThat(communityPostMapper.selectById(postId).getCommentCount()).isEqualTo(1);
    }

    @Test
    void listCommentsExcludesDeletedComments() throws Exception {
        String token = registerAndLogin("comment_list_alice", "comment_list_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "评论列表", "删除后的评论不应该展示。");
        Long deletedCommentId = createComment(token, postId, "稍后删除");
        Long visibleCommentId = createComment(token, postId, "保留评论");

        mockMvc.perform(delete("/api/community/comments/{commentId}", deletedCommentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(deletedCommentId.intValue()))))
                .andExpect(jsonPath("$.data[*].id", hasItem(visibleCommentId.intValue())));
    }

    @Test
    void commentsCanBeReadWithoutLoginButWritingRequiresLogin() throws Exception {
        String token = registerAndLogin("comment_public_read_alice", "comment_public_read_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "公开评论", "游客可以看评论，但不能直接评论。");
        Long commentId = createComment(token, postId, "这条评论游客也能看到。");

        mockMvc.perform(get("/api/community/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(commentId.intValue())));

        mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "游客不能直接评论"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationRejectsBlankAndOverlongCommentContent() throws Exception {
        String token = registerAndLogin("comment_validation_alice", "comment_validation_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "评论校验", "评论内容需要校验。");

        mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("content", "a".repeat(2001))
                                .toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void commentOnDeletedPostFails() throws Exception {
        String token = registerAndLogin("comment_deleted_post_alice", "comment_deleted_post_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "删除帖子", "删除后不能评论。");

        mockMvc.perform(delete("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "不能创建"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCommentOnDeletedPostFailsAndKeepsCommentCount() throws Exception {
        String token = registerAndLogin("comment_delete_after_post_alice", "comment_delete_after_post_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Deleted post comment delete", "Cannot delete comment after post deletion.");
        Long commentId = createComment(token, postId, "Keep count after post deletion.");

        mockMvc.perform(delete("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/community/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());

        Integer commentCount = communityPostMapper.selectById(postId).getCommentCount();
        org.assertj.core.api.Assertions.assertThat(commentCount).isEqualTo(1);
    }

    @Test
    void doubleDeleteDoesNotDecrementCommentCountBelowZero() throws Exception {
        String token = registerAndLogin("comment_double_delete_alice", "comment_double_delete_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "重复删除", "评论计数不能低于 0。");
        Long commentId = createComment(token, postId, "只计算一次");

        mockMvc.perform(delete("/api/community/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/community/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentCount").value(0));

        assertThat(communityPostMapper.selectById(postId).getCommentCount()).isZero();
    }

    @Test
    void incrementCommentCountOnDeletedPostFailsAndKeepsCount() throws Exception {
        String token = registerAndLogin("comment_increment_deleted_post_alice", "comment_increment_deleted_post_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Deleted post increment", "Count increment should require published post.");

        mockMvc.perform(delete("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThatThrownBy(() -> communityPostService.incrementCommentCount(postId, LocalDateTime.now()))
                .isInstanceOf(BusinessException.class);
        assertThat(communityPostMapper.selectById(postId).getCommentCount()).isZero();
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

    private Long createComment(String token, Long postId, String content) throws Exception {
        MvcResult commentResult = mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "%s"
                                }
                                """.formatted(content)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(commentResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private Long extractUserId(String token) throws Exception {
        String[] parts = token.split("\\.");
        JsonNode payload = objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(parts[1]));
        return payload.path("userId").asLong();
    }

    private void setDefaultMembershipStatus(Long userId, String status) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getUserId, userId));
        member.setStatus(status);
        circleMemberMapper.updateById(member);
    }
}
