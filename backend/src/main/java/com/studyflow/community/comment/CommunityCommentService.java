package com.studyflow.community.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.comment.dto.CommunityCommentRequest;
import com.studyflow.community.comment.dto.CommunityCommentResponse;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.member.UserProfile;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostService;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityCommentService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DELETED = "DELETED";

    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityPostService communityPostService;
    private final CommunityMemberService communityMemberService;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    public CommunityCommentService(
            CommunityCommentMapper communityCommentMapper,
            CommunityPostService communityPostService,
            CommunityMemberService communityMemberService,
            UserProfileMapper userProfileMapper,
            UserMapper userMapper
    ) {
        this.communityCommentMapper = communityCommentMapper;
        this.communityPostService = communityPostService;
        this.communityMemberService = communityMemberService;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
    }

    public List<CommunityCommentResponse> listComments(Long userId, Long postId) {
        Circle circle = communityMemberService.getDefaultCircle();
        communityPostService.requirePublishedPost(circle.getId(), postId);
        List<CommunityComment> comments = communityCommentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getCircleId, circle.getId())
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getStatus, STATUS_PUBLISHED)
                .orderByAsc(CommunityComment::getCreatedAt)
                .orderByAsc(CommunityComment::getId));
        return toResponses(comments);
    }

    @Transactional
    public CommunityCommentResponse createComment(Long userId, Long postId, CommunityCommentRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = communityPostService.requirePublishedPost(circle.getId(), postId);
        LocalDateTime now = LocalDateTime.now();

        CommunityComment comment = new CommunityComment();
        comment.setCircleId(circle.getId());
        comment.setPostId(post.getId());
        comment.setAuthorId(userId);
        comment.setParentId(null);
        comment.setContent(request.content());
        comment.setStatus(STATUS_PUBLISHED);
        comment.setReactionCount(0);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        communityCommentMapper.insert(comment);
        communityPostService.incrementCommentCount(post.getId(), now);
        return toResponse(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityComment comment = communityCommentMapper.selectOne(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getId, commentId)
                .eq(CommunityComment::getCircleId, circle.getId())
                .eq(CommunityComment::getStatus, STATUS_PUBLISHED));
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        communityPostService.requirePublishedPost(circle.getId(), comment.getPostId());
        if (!comment.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "没有权限操作这条评论");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = communityCommentMapper.update(null, new LambdaUpdateWrapper<CommunityComment>()
                .eq(CommunityComment::getId, comment.getId())
                .eq(CommunityComment::getCircleId, circle.getId())
                .eq(CommunityComment::getAuthorId, userId)
                .eq(CommunityComment::getStatus, STATUS_PUBLISHED)
                .set(CommunityComment::getStatus, STATUS_DELETED)
                .set(CommunityComment::getDeletedAt, now)
                .set(CommunityComment::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(404, "评论不存在");
        }
        communityPostService.decrementCommentCount(comment.getPostId(), now);
    }

    private List<CommunityCommentResponse> toResponses(List<CommunityComment> comments) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = comments.stream()
                .map(CommunityComment::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, AuthorDisplay> authorDisplays = authorDisplays(authorIds);
        return comments.stream()
                .map(comment -> toResponse(comment, authorDisplays))
                .toList();
    }

    private CommunityCommentResponse toResponse(CommunityComment comment) {
        return toResponses(List.of(comment)).get(0);
    }

    private CommunityCommentResponse toResponse(CommunityComment comment, Map<Long, AuthorDisplay> authorDisplays) {
        AuthorDisplay author = authorDisplays.getOrDefault(comment.getAuthorId(), new AuthorDisplay("", null));
        return new CommunityCommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                author.name(),
                author.avatarUrl(),
                comment.getContent(),
                comment.getStatus(),
                comment.getReactionCount(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private Map<Long, AuthorDisplay> authorDisplays(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, AuthorDisplay> authors = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, authorIds))
                .stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> new AuthorDisplay(user.getUsername(), null)
                ));
        userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getUserId, authorIds))
                .forEach(profile -> {
                    AuthorDisplay current = authors.getOrDefault(profile.getUserId(), new AuthorDisplay("", null));
                    String displayName = profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
                            ? profile.getDisplayName()
                            : current.name();
                    String avatarUrl = profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()
                            ? profile.getAvatarUrl()
                            : current.avatarUrl();
                    authors.put(profile.getUserId(), new AuthorDisplay(displayName, avatarUrl));
                });
        return authors;
    }

    private record AuthorDisplay(String name, String avatarUrl) {
    }
}
