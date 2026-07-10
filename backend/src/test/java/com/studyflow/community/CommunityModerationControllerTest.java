package com.studyflow.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import com.studyflow.community.moderation.CommunityModerationAction;
import com.studyflow.community.moderation.CommunityModerationActionMapper;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CommunityModerationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CircleMemberMapper circleMemberMapper;

    @Autowired
    private CommunityModerationActionMapper moderationActionMapper;

    @Test
    void memberCannotHidePost() throws Exception {
        String aliceToken = registerAndLogin("moderation_member_alice", "moderation_member_alice@example.com");
        Long topicId = firstTopicId(aliceToken);
        Long postId = createPost(aliceToken, topicId, "普通用户不能隐藏", "管理员才可以隐藏帖子。");

        mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "member should fail"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanHideAndRestorePost() throws Exception {
        String adminToken = registerAdminAndLogin("moderation_admin", "moderation_admin@example.com");
        String memberToken = registerAndLogin("moderation_author", "moderation_author@example.com");
        Long topicId = firstTopicId(memberToken);
        Long postId = createPost(memberToken, topicId, "管理员审核", "这条帖子会被隐藏再恢复。");

        mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "测试隐藏"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(post("/api/admin/community/posts/{postId}/restore", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "测试恢复"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void adminActionCreatesAuditRow() throws Exception {
        String adminToken = registerAdminAndLogin("moderation_audit_admin", "moderation_audit_admin@example.com");
        String memberToken = registerAndLogin("moderation_audit_author", "moderation_audit_author@example.com");
        Long postId = createPost(memberToken, firstTopicId(memberToken), "审核日志", "管理员操作需要留下日志。");

        mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "audit check"
                                }
                                """))
                .andExpect(status().isOk());

        CommunityModerationAction action = moderationActionMapper.selectOne(new LambdaQueryWrapper<CommunityModerationAction>()
                .eq(CommunityModerationAction::getTargetType, "POST")
                .eq(CommunityModerationAction::getTargetId, postId)
                .eq(CommunityModerationAction::getActionType, "HIDE"));
        assertThat(action).isNotNull();
        assertThat(action.getReason()).isEqualTo("audit check");
    }

    @Test
    void adminCanHideAndRestoreComment() throws Exception {
        String adminToken = registerAdminAndLogin("moderation_comment_admin", "moderation_comment_admin@example.com");
        String memberToken = registerAndLogin("moderation_comment_author", "moderation_comment_author@example.com");
        Long postId = createPost(memberToken, firstTopicId(memberToken), "评论审核", "评论会被隐藏再恢复。");
        Long commentId = createComment(memberToken, postId, "这条评论会被审核。");

        mockMvc.perform(post("/api/admin/community/comments/{commentId}/hide", commentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "隐藏评论"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(post("/api/admin/community/comments/{commentId}/restore", commentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "恢复评论"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void duplicatePostModerationTransitionDoesNotCreateExtraAuditRow() throws Exception {
        String adminToken = registerAdminAndLogin("moderation_cas_admin", "moderation_cas_admin@example.com");
        String memberToken = registerAndLogin("moderation_cas_author", "moderation_cas_author@example.com");
        Long postId = createPost(memberToken, firstTopicId(memberToken), "重复审核", "重复隐藏不应该重复记录。");

        mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "first hide"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "duplicate hide"
                                }
                                """))
                .andExpect(status().is4xxClientError());

        assertThat(moderationActionCount("POST", postId, "HIDE")).isEqualTo(1);

        mockMvc.perform(post("/api/admin/community/posts/{postId}/restore", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "restore"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(moderationActionCount("POST", postId, "RESTORE")).isEqualTo(1);
    }

    @Test
    void mutedMemberCannotCreatePostOrCommentThenCanAfterUnmute() throws Exception {
        String adminToken = registerAdminAndLogin("moderation_mute_admin", "moderation_mute_admin@example.com");
        String memberToken = registerAndLogin("moderation_muted_member", "moderation_muted_member@example.com");
        Long memberUserId = userIdByUsername("moderation_muted_member");
        Long topicId = firstTopicId(memberToken);
        Long existingPostId = createPost(memberToken, topicId, "禁言前帖子", "禁言前可正常发帖。");

        mockMvc.perform(post("/api/admin/community/members/{userId}/mute", memberUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "测试禁言"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "禁言后帖子",
                                  "content": "禁言后不能发帖。"
                                }
                                """.formatted(topicId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/community/posts/{postId}/comments", existingPostId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "禁言后不能评论。"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/community/members/{userId}/unmute", memberUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "测试解除禁言"
                                }
                                """))
                .andExpect(status().isOk());

        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getUserId, memberUserId));
        assertThat(member.getStatus()).isEqualTo("ACTIVE");
        assertThat(moderationActionCount("MEMBER", memberUserId, "MUTE")).isEqualTo(1);
        assertThat(moderationActionCount("MEMBER", memberUserId, "UNMUTE")).isEqualTo(1);
        createComment(memberToken, existingPostId, "解除禁言后可以评论。");
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        registerAndLogin("moderation_disabled_user", "moderation_disabled_user@example.com");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "moderation_disabled_user"));
        user.setStatus("DISABLED");
        userMapper.updateById(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "moderation_disabled_user",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledUserCannotUseExistingToken() throws Exception {
        String token = registerAndLogin("moderation_disabled_token", "moderation_disabled_token@example.com");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "moderation_disabled_token"));
        user.setStatus("DISABLED");
        userMapper.updateById(user);

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledCircleAdminCannotModerate() throws Exception {
        String adminToken = registerAdminAndLogin("moderation_disabled_circle_admin", "moderation_disabled_circle_admin@example.com");
        String memberToken = registerAndLogin("moderation_disabled_circle_author", "moderation_disabled_circle_author@example.com");
        Long postId = createPost(memberToken, firstTopicId(memberToken), "Disabled circle admin", "Disabled circle members cannot moderate.");
        Long adminUserId = userIdByUsername("moderation_disabled_circle_admin");
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getUserId, adminUserId));
        member.setStatus("DISABLED");
        circleMemberMapper.updateById(member);

        mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "disabled circle member should fail"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String registerAdminAndLogin(String username, String email) throws Exception {
        String token = registerAndLogin(username, email);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        user.setRole("ADMIN");
        userMapper.updateById(user);
        return token;
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

    private Long userIdByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username))
                .getId();
    }

    private Long moderationActionCount(String targetType, Long targetId, String actionType) {
        return moderationActionMapper.selectCount(new LambdaQueryWrapper<CommunityModerationAction>()
                .eq(CommunityModerationAction::getTargetType, targetType)
                .eq(CommunityModerationAction::getTargetId, targetId)
                .eq(CommunityModerationAction::getActionType, actionType));
    }
}
