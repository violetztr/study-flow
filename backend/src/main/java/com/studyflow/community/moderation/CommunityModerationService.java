package com.studyflow.community.moderation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.comment.CommunityComment;
import com.studyflow.community.comment.CommunityCommentMapper;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.moderation.dto.ModerationRequest;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.community.topic.CommunityTopicMapper;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CommunityModerationService {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OWNER = "OWNER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_MUTED = "MUTED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_HIDDEN = "HIDDEN";
    private static final String TARGET_POST = "POST";
    private static final String TARGET_COMMENT = "COMMENT";
    private static final String TARGET_MEMBER = "MEMBER";
    private static final String ACTION_HIDE = "HIDE";
    private static final String ACTION_RESTORE = "RESTORE";
    private static final String ACTION_MUTE = "MUTE";
    private static final String ACTION_UNMUTE = "UNMUTE";

    private final CommunityMemberService communityMemberService;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityCommentMapper communityCommentMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final CommunityTopicMapper communityTopicMapper;
    private final CommunityModerationActionMapper moderationActionMapper;
    private final UserMapper userMapper;

    public CommunityModerationService(
            CommunityMemberService communityMemberService,
            CommunityPostMapper communityPostMapper,
            CommunityCommentMapper communityCommentMapper,
            CircleMemberMapper circleMemberMapper,
            CommunityTopicMapper communityTopicMapper,
            CommunityModerationActionMapper moderationActionMapper,
            UserMapper userMapper
    ) {
        this.communityMemberService = communityMemberService;
        this.communityPostMapper = communityPostMapper;
        this.communityCommentMapper = communityCommentMapper;
        this.circleMemberMapper = circleMemberMapper;
        this.communityTopicMapper = communityTopicMapper;
        this.moderationActionMapper = moderationActionMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public void hidePost(Long adminUserId, Long postId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityPost post = requirePost(circle.getId(), postId, STATUS_PUBLISHED);
        LocalDateTime now = LocalDateTime.now();
        updatePostStatus(post.getId(), STATUS_PUBLISHED, STATUS_HIDDEN, now);
        if (post.getTopicId() != null) {
            communityTopicMapper.decrementPostCount(post.getTopicId());
        }
        recordAction(circle.getId(), adminUserId, TARGET_POST, postId, ACTION_HIDE, reason(request), now);
    }

    @Transactional
    public void restorePost(Long adminUserId, Long postId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityPost post = requirePost(circle.getId(), postId, STATUS_HIDDEN);
        LocalDateTime now = LocalDateTime.now();
        updatePostStatus(post.getId(), STATUS_HIDDEN, STATUS_PUBLISHED, now);
        if (post.getTopicId() != null) {
            communityTopicMapper.incrementPostCount(post.getTopicId());
        }
        recordAction(circle.getId(), adminUserId, TARGET_POST, postId, ACTION_RESTORE, reason(request), now);
    }

    @Transactional
    public void hideComment(Long adminUserId, Long commentId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityComment comment = requireComment(circle.getId(), commentId, STATUS_PUBLISHED);
        LocalDateTime now = LocalDateTime.now();
        updateCommentStatus(comment.getId(), STATUS_PUBLISHED, STATUS_HIDDEN, now);
        decrementCommentCount(comment.getPostId(), now);
        recordAction(circle.getId(), adminUserId, TARGET_COMMENT, commentId, ACTION_HIDE, reason(request), now);
    }

    @Transactional
    public void restoreComment(Long adminUserId, Long commentId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityComment comment = requireComment(circle.getId(), commentId, STATUS_HIDDEN);
        LocalDateTime now = LocalDateTime.now();
        updateCommentStatus(comment.getId(), STATUS_HIDDEN, STATUS_PUBLISHED, now);
        incrementCommentCount(comment.getPostId(), now);
        recordAction(circle.getId(), adminUserId, TARGET_COMMENT, commentId, ACTION_RESTORE, reason(request), now);
    }

    @Transactional
    public void muteMember(Long adminUserId, Long userId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        LocalDateTime now = LocalDateTime.now();
        updateMemberStatus(circle.getId(), userId, STATUS_MUTED, now);
        recordAction(circle.getId(), adminUserId, TARGET_MEMBER, userId, ACTION_MUTE, reason(request), now);
    }

    @Transactional
    public void unmuteMember(Long adminUserId, Long userId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        LocalDateTime now = LocalDateTime.now();
        updateMemberStatus(circle.getId(), userId, STATUS_ACTIVE, now);
        recordAction(circle.getId(), adminUserId, TARGET_MEMBER, userId, ACTION_UNMUTE, reason(request), now);
    }

    private Circle requireAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !(ROLE_ADMIN.equals(user.getRole()) || ROLE_OWNER.equals(user.getRole()))) {
            throw new BusinessException(403, "需要管理员权限");
        }
        return communityMemberService.requireActiveDefaultMember(userId);
    }

    private CommunityPost requirePost(Long circleId, Long postId, String status) {
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circleId)
                .eq(CommunityPost::getStatus, status));
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        return post;
    }

    private CommunityComment requireComment(Long circleId, Long commentId, String status) {
        CommunityComment comment = communityCommentMapper.selectOne(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getId, commentId)
                .eq(CommunityComment::getCircleId, circleId)
                .eq(CommunityComment::getStatus, status));
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        return comment;
    }

    private CircleMember requireCircleMember(Long circleId, Long userId) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(404, "圈子成员不存在");
        }
        return member;
    }

    private void updatePostStatus(Long postId, String expectedStatus, String status, LocalDateTime now) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, expectedStatus)
                .set(CommunityPost::getStatus, status)
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Post status changed");
        }
    }

    private void updateCommentStatus(Long commentId, String expectedStatus, String status, LocalDateTime now) {
        int updated = communityCommentMapper.update(null, new LambdaUpdateWrapper<CommunityComment>()
                .eq(CommunityComment::getId, commentId)
                .eq(CommunityComment::getStatus, expectedStatus)
                .set(CommunityComment::getStatus, status)
                .set(CommunityComment::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Comment status changed");
        }
    }

    private void updateMemberStatus(Long circleId, Long userId, String status, LocalDateTime now) {
        int updated = circleMemberMapper.update(null, new LambdaUpdateWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId)
                .set(CircleMember::getStatus, status)
                .set(CircleMember::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(404, "圈子成员不存在");
        }
    }

    private void incrementCommentCount(Long postId, LocalDateTime now) {
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("comment_count = comment_count + 1")
                .set(CommunityPost::getUpdatedAt, now));
    }

    private void decrementCommentCount(Long postId, LocalDateTime now) {
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("comment_count = CASE WHEN comment_count > 0 THEN comment_count - 1 ELSE 0 END")
                .set(CommunityPost::getUpdatedAt, now));
    }

    private void recordAction(
            Long circleId,
            Long adminUserId,
            String targetType,
            Long targetId,
            String actionType,
            String reason,
            LocalDateTime now
    ) {
        CommunityModerationAction action = new CommunityModerationAction();
        action.setCircleId(circleId);
        action.setAdminUserId(adminUserId);
        action.setTargetType(targetType);
        action.setTargetId(targetId);
        action.setActionType(actionType);
        action.setReason(reason);
        action.setCreatedAt(now);
        moderationActionMapper.insert(action);
    }

    private String reason(ModerationRequest request) {
        return request == null ? null : request.reason();
    }
}
