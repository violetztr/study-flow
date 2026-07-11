package com.studyflow.community.moderation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.comment.CommunityComment;
import com.studyflow.community.comment.CommunityCommentMapper;
import com.studyflow.community.danmaku.CommunityDanmaku;
import com.studyflow.community.danmaku.CommunityDanmakuMapper;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.moderation.dto.ModerationRequest;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostCacheService;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.community.post.CommunityPostService;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.community.topic.CommunityTopicMapper;
import com.studyflow.media.MediaService;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommunityModerationService {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OWNER = "OWNER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_MUTED = "MUTED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_HIDDEN = "HIDDEN";
    private static final String STATUS_DELETED = "DELETED";
    private static final String TARGET_POST = "POST";
    private static final String TARGET_COMMENT = "COMMENT";
    private static final String TARGET_DANMAKU = "DANMAKU";
    private static final String TARGET_MEMBER = "MEMBER";
    private static final String ACTION_HIDE = "HIDE";
    private static final String ACTION_RESTORE = "RESTORE";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_MUTE = "MUTE";
    private static final String ACTION_UNMUTE = "UNMUTE";

    private final CommunityMemberService communityMemberService;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityPostService communityPostService;
    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityDanmakuMapper communityDanmakuMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final CommunityTopicMapper communityTopicMapper;
    private final CommunityModerationActionMapper moderationActionMapper;
    private final MediaService mediaService;
    private final UserMapper userMapper;
    private final CommunityPostCacheService communityPostCacheService;

    public CommunityModerationService(
            CommunityMemberService communityMemberService,
            CommunityPostMapper communityPostMapper,
            CommunityPostService communityPostService,
            CommunityCommentMapper communityCommentMapper,
            CommunityDanmakuMapper communityDanmakuMapper,
            CircleMemberMapper circleMemberMapper,
            CommunityTopicMapper communityTopicMapper,
            CommunityModerationActionMapper moderationActionMapper,
            MediaService mediaService,
            UserMapper userMapper,
            CommunityPostCacheService communityPostCacheService
    ) {
        this.communityMemberService = communityMemberService;
        this.communityPostMapper = communityPostMapper;
        this.communityPostService = communityPostService;
        this.communityCommentMapper = communityCommentMapper;
        this.communityDanmakuMapper = communityDanmakuMapper;
        this.circleMemberMapper = circleMemberMapper;
        this.communityTopicMapper = communityTopicMapper;
        this.moderationActionMapper = moderationActionMapper;
        this.mediaService = mediaService;
        this.userMapper = userMapper;
        this.communityPostCacheService = communityPostCacheService;
    }

    public List<CommunityPostResponse> listPendingSubmissions(Long adminUserId) {
        Circle circle = requireAdmin(adminUserId);
        return communityPostService.listPendingReviewSubmissions(adminUserId, circle.getId());
    }

    @Transactional
    public void approveSubmission(Long adminUserId, Long postId) {
        Circle circle = requireAdmin(adminUserId);
        CommunityPost post = requirePost(circle.getId(), postId, STATUS_PENDING_REVIEW);
        LocalDateTime now = LocalDateTime.now();
        updatePostReviewStatus(post.getId(), STATUS_PENDING_REVIEW, STATUS_PUBLISHED, adminUserId, null, now);
        mediaService.approveAttachedVideosForPost(post.getId(), now);
        if (post.getTopicId() != null) {
            communityTopicMapper.incrementPostCount(post.getTopicId());
        }
        recordAction(circle.getId(), adminUserId, TARGET_POST, postId, ACTION_APPROVE, null, now);
        communityPostCacheService.evictFeedAndPost(postId);
    }

    @Transactional
    public void rejectSubmission(Long adminUserId, Long postId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityPost post = requirePost(circle.getId(), postId, STATUS_PENDING_REVIEW);
        LocalDateTime now = LocalDateTime.now();
        String reason = requiredReason(request);
        updatePostReviewStatus(post.getId(), STATUS_PENDING_REVIEW, STATUS_REJECTED, adminUserId, reason, now);
        mediaService.rejectAttachedVideosForPost(post.getId(), now);
        recordAction(circle.getId(), adminUserId, TARGET_POST, postId, ACTION_REJECT, reason, now);
        communityPostCacheService.evictFeedAndPost(postId);
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
        communityPostCacheService.evictFeedAndPost(postId);
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
        communityPostCacheService.evictFeedAndPost(postId);
    }

    @Transactional
    public void deletePost(Long adminUserId, Long postId) {
        Circle circle = requireAdmin(adminUserId);
        CommunityPost post = requirePostNotDeleted(circle.getId(), postId);
        LocalDateTime now = LocalDateTime.now();
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, post.getId())
                .eq(CommunityPost::getCircleId, circle.getId())
                .ne(CommunityPost::getStatus, STATUS_DELETED)
                .set(CommunityPost::getStatus, STATUS_DELETED)
                .set(CommunityPost::getDeletedAt, now)
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Post status changed");
        }
        if (STATUS_PUBLISHED.equals(post.getStatus()) && post.getTopicId() != null) {
            communityTopicMapper.decrementPostCount(post.getTopicId());
        }
        recordAction(circle.getId(), adminUserId, TARGET_POST, postId, ACTION_DELETE, null, now);
        communityPostCacheService.evictFeedAndPost(postId);
    }

    @Transactional
    public void hideComment(Long adminUserId, Long commentId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityComment comment = requireComment(circle.getId(), commentId, STATUS_PUBLISHED);
        LocalDateTime now = LocalDateTime.now();
        updateCommentStatus(comment.getId(), STATUS_PUBLISHED, STATUS_HIDDEN, now);
        decrementCommentCount(comment.getPostId(), now);
        recordAction(circle.getId(), adminUserId, TARGET_COMMENT, commentId, ACTION_HIDE, reason(request), now);
        communityPostCacheService.evictFeedAndPost(comment.getPostId());
    }

    @Transactional
    public void restoreComment(Long adminUserId, Long commentId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        CommunityComment comment = requireComment(circle.getId(), commentId, STATUS_HIDDEN);
        LocalDateTime now = LocalDateTime.now();
        updateCommentStatus(comment.getId(), STATUS_HIDDEN, STATUS_PUBLISHED, now);
        incrementCommentCount(comment.getPostId(), now);
        recordAction(circle.getId(), adminUserId, TARGET_COMMENT, commentId, ACTION_RESTORE, reason(request), now);
        communityPostCacheService.evictFeedAndPost(comment.getPostId());
    }

    @Transactional
    public void deleteComment(Long adminUserId, Long commentId) {
        Circle circle = requireAdmin(adminUserId);
        CommunityComment comment = requireCommentNotDeleted(circle.getId(), commentId);
        LocalDateTime now = LocalDateTime.now();
        int updated = communityCommentMapper.update(null, new LambdaUpdateWrapper<CommunityComment>()
                .eq(CommunityComment::getId, comment.getId())
                .eq(CommunityComment::getCircleId, circle.getId())
                .ne(CommunityComment::getStatus, STATUS_DELETED)
                .set(CommunityComment::getStatus, STATUS_DELETED)
                .set(CommunityComment::getDeletedAt, now)
                .set(CommunityComment::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Comment status changed");
        }
        if (STATUS_PUBLISHED.equals(comment.getStatus())) {
            decrementCommentCount(comment.getPostId(), now);
        }
        recordAction(circle.getId(), adminUserId, TARGET_COMMENT, commentId, ACTION_DELETE, null, now);
        communityPostCacheService.evictFeedAndPost(comment.getPostId());
    }

    @Transactional
    public void deleteDanmaku(Long adminUserId, Long danmakuId) {
        Circle circle = requireAdmin(adminUserId);
        CommunityDanmaku danmaku = requireDanmakuNotDeleted(circle.getId(), danmakuId);
        LocalDateTime now = LocalDateTime.now();
        int updated = communityDanmakuMapper.update(null, new LambdaUpdateWrapper<CommunityDanmaku>()
                .eq(CommunityDanmaku::getId, danmaku.getId())
                .ne(CommunityDanmaku::getStatus, STATUS_DELETED)
                .set(CommunityDanmaku::getStatus, STATUS_DELETED));
        if (updated != 1) {
            throw new BusinessException(409, "Danmaku status changed");
        }
        recordAction(circle.getId(), adminUserId, TARGET_DANMAKU, danmakuId, ACTION_DELETE, null, now);
        communityPostCacheService.evictFeedAndPost(danmaku.getPostId());
    }

    @Transactional
    public void muteMember(Long adminUserId, Long userId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        requireMemberModerationAllowed(circle.getId(), adminUserId, userId, STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        updateMemberStatus(circle.getId(), userId, STATUS_ACTIVE, STATUS_MUTED, now);
        recordAction(circle.getId(), adminUserId, TARGET_MEMBER, userId, ACTION_MUTE, reason(request), now);
    }

    @Transactional
    public void unmuteMember(Long adminUserId, Long userId, ModerationRequest request) {
        Circle circle = requireAdmin(adminUserId);
        requireMemberModerationAllowed(circle.getId(), adminUserId, userId, STATUS_MUTED);
        LocalDateTime now = LocalDateTime.now();
        updateMemberStatus(circle.getId(), userId, STATUS_MUTED, STATUS_ACTIVE, now);
        recordAction(circle.getId(), adminUserId, TARGET_MEMBER, userId, ACTION_UNMUTE, reason(request), now);
    }

    private Circle requireAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !(ROLE_ADMIN.equals(user.getRole()) || ROLE_OWNER.equals(user.getRole()))) {
            throw new BusinessException(403, "Admin permission required");
        }
        return communityMemberService.requireActiveDefaultMember(userId);
    }

    private CommunityPost requirePost(Long circleId, Long postId, String status) {
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circleId)
                .eq(CommunityPost::getStatus, status));
        if (post == null) {
            throw new BusinessException(404, "Post does not exist");
        }
        return post;
    }

    private CommunityPost requirePostNotDeleted(Long circleId, Long postId) {
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circleId)
                .ne(CommunityPost::getStatus, STATUS_DELETED));
        if (post == null) {
            throw new BusinessException(404, "Post does not exist");
        }
        return post;
    }

    private CommunityComment requireComment(Long circleId, Long commentId, String status) {
        CommunityComment comment = communityCommentMapper.selectOne(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getId, commentId)
                .eq(CommunityComment::getCircleId, circleId)
                .eq(CommunityComment::getStatus, status));
        if (comment == null) {
            throw new BusinessException(404, "Comment does not exist");
        }
        return comment;
    }

    private CommunityComment requireCommentNotDeleted(Long circleId, Long commentId) {
        CommunityComment comment = communityCommentMapper.selectOne(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getId, commentId)
                .eq(CommunityComment::getCircleId, circleId)
                .ne(CommunityComment::getStatus, STATUS_DELETED));
        if (comment == null) {
            throw new BusinessException(404, "Comment does not exist");
        }
        return comment;
    }

    private CommunityDanmaku requireDanmakuNotDeleted(Long circleId, Long danmakuId) {
        CommunityDanmaku danmaku = communityDanmakuMapper.selectOne(new LambdaQueryWrapper<CommunityDanmaku>()
                .eq(CommunityDanmaku::getId, danmakuId)
                .ne(CommunityDanmaku::getStatus, STATUS_DELETED));
        if (danmaku == null) {
            throw new BusinessException(404, "Danmaku does not exist");
        }
        CommunityPost post = communityPostMapper.selectById(danmaku.getPostId());
        if (post == null || !circleId.equals(post.getCircleId())) {
            throw new BusinessException(404, "Danmaku does not exist");
        }
        return danmaku;
    }

    private CircleMember requireCircleMember(Long circleId, Long userId) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(404, "Circle member does not exist");
        }
        return member;
    }

    private void requireMemberModerationAllowed(Long circleId, Long adminUserId, Long targetUserId, String expectedStatus) {
        if (adminUserId.equals(targetUserId)) {
            throw new BusinessException(403, "Cannot moderate yourself");
        }

        CircleMember targetMember = requireCircleMember(circleId, targetUserId);
        if (!expectedStatus.equals(targetMember.getStatus())) {
            throw new BusinessException(409, "Member status changed");
        }

        User adminUser = userMapper.selectById(adminUserId);
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "User does not exist");
        }
        if (ROLE_ADMIN.equals(adminUser.getRole()) && (ROLE_ADMIN.equals(targetUser.getRole()) || ROLE_OWNER.equals(targetUser.getRole()))) {
            throw new BusinessException(403, "Admin cannot moderate peer or owner accounts");
        }
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

    private void updatePostReviewStatus(
            Long postId,
            String expectedStatus,
            String status,
            Long reviewedBy,
            String reviewReason,
            LocalDateTime now
    ) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, expectedStatus)
                .set(CommunityPost::getStatus, status)
                .set(CommunityPost::getReviewedBy, reviewedBy)
                .set(CommunityPost::getReviewedAt, now)
                .set(CommunityPost::getReviewReason, reviewReason)
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

    private void updateMemberStatus(Long circleId, Long userId, String expectedStatus, String status, LocalDateTime now) {
        int updated = circleMemberMapper.update(null, new LambdaUpdateWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId)
                .eq(CircleMember::getStatus, expectedStatus)
                .set(CircleMember::getStatus, status)
                .set(CircleMember::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Member status changed");
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

    private String requiredReason(ModerationRequest request) {
        String reason = reason(request);
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(400, "Reject reason is required");
        }
        return reason.trim();
    }
}
