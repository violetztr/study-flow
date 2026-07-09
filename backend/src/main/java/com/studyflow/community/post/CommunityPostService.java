package com.studyflow.community.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.circle.CircleMapper;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import com.studyflow.community.member.UserProfile;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.community.post.dto.CommunityPostRequest;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.community.topic.CommunityTopic;
import com.studyflow.community.topic.CommunityTopicMapper;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.studyflow.community.member.CommunityMemberService.DEFAULT_CIRCLE_SLUG;
import static com.studyflow.community.member.CommunityMemberService.STATUS_ACTIVE;

@Service
public class CommunityPostService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String CONTENT_FORMAT_TEXT = "TEXT";
    private static final String VISIBILITY_CIRCLE = "CIRCLE";

    private final CommunityPostMapper communityPostMapper;
    private final CommunityTopicMapper communityTopicMapper;
    private final CircleMapper circleMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    public CommunityPostService(
            CommunityPostMapper communityPostMapper,
            CommunityTopicMapper communityTopicMapper,
            CircleMapper circleMapper,
            CircleMemberMapper circleMemberMapper,
            UserProfileMapper userProfileMapper,
            UserMapper userMapper
    ) {
        this.communityPostMapper = communityPostMapper;
        this.communityTopicMapper = communityTopicMapper;
        this.circleMapper = circleMapper;
        this.circleMemberMapper = circleMemberMapper;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
    }

    public List<CommunityPostResponse> listFeed(Long userId) {
        Circle circle = getDefaultCircle();
        return communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getCircleId, circle.getId())
                        .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                        .orderByDesc(CommunityPost::getPinned)
                        .orderByDesc(CommunityPost::getLastActivityAt)
                        .orderByDesc(CommunityPost::getCreatedAt))
                .stream()
                .map(post -> toResponse(post, userId))
                .toList();
    }

    public CommunityPostResponse getPost(Long userId, Long postId) {
        Circle circle = getDefaultCircle();
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED));
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        return toResponse(post, userId);
    }

    @Transactional
    public CommunityPostResponse createPost(Long userId, CommunityPostRequest request) {
        Circle circle = getDefaultCircle();
        requireActiveMember(circle.getId(), userId);
        CommunityTopic topic = findActiveTopic(circle.getId(), request.topicId());
        LocalDateTime now = LocalDateTime.now();

        CommunityPost post = new CommunityPost();
        post.setCircleId(circle.getId());
        post.setAuthorId(userId);
        post.setTopicId(topic == null ? null : topic.getId());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setContentFormat(CONTENT_FORMAT_TEXT);
        post.setVisibility(VISIBILITY_CIRCLE);
        post.setStatus(STATUS_PUBLISHED);
        post.setPinned(false);
        post.setCommentCount(0);
        post.setReactionCount(0);
        post.setViewCount(0);
        post.setLastActivityAt(now);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        communityPostMapper.insert(post);
        return toResponse(post, userId);
    }

    @Transactional
    public CommunityPostResponse updatePost(Long userId, Long postId, CommunityPostRequest request) {
        Circle circle = getDefaultCircle();
        CommunityPost post = requireOwnedPost(circle.getId(), userId, postId);
        CommunityTopic topic = findActiveTopic(circle.getId(), request.topicId());

        post.setTopicId(topic == null ? null : topic.getId());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setLastActivityAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        communityPostMapper.updateById(post);
        return toResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Circle circle = getDefaultCircle();
        CommunityPost post = requireOwnedPost(circle.getId(), userId, postId);
        LocalDateTime now = LocalDateTime.now();
        post.setStatus(STATUS_DELETED);
        post.setDeletedAt(now);
        post.setUpdatedAt(now);
        communityPostMapper.updateById(post);
    }

    private Circle getDefaultCircle() {
        Circle circle = circleMapper.selectOne(new LambdaQueryWrapper<Circle>()
                .eq(Circle::getSlug, DEFAULT_CIRCLE_SLUG));
        if (circle == null) {
            throw new BusinessException(500, "默认圈子不存在");
        }
        return circle;
    }

    private void requireActiveMember(Long circleId, Long userId) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId)
                .eq(CircleMember::getStatus, STATUS_ACTIVE));
        if (member == null) {
            throw new BusinessException(403, "没有权限操作这条帖子");
        }
    }

    private CommunityTopic findActiveTopic(Long circleId, Long topicId) {
        if (topicId == null) {
            return null;
        }
        CommunityTopic topic = communityTopicMapper.selectOne(new LambdaQueryWrapper<CommunityTopic>()
                .eq(CommunityTopic::getId, topicId)
                .eq(CommunityTopic::getCircleId, circleId)
                .eq(CommunityTopic::getStatus, STATUS_ACTIVE));
        if (topic == null) {
            throw new BusinessException(404, "话题不存在");
        }
        return topic;
    }

    private CommunityPost requireOwnedPost(Long circleId, Long userId, Long postId) {
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circleId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED));
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "没有权限操作这条帖子");
        }
        return post;
    }

    private CommunityPostResponse toResponse(CommunityPost post, Long currentUserId) {
        CommunityTopic topic = post.getTopicId() == null ? null : communityTopicMapper.selectById(post.getTopicId());
        return new CommunityPostResponse(
                post.getId(),
                post.getCircleId(),
                post.getAuthorId(),
                authorName(post.getAuthorId()),
                post.getTopicId(),
                topic == null ? null : topic.getName(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getPinned(),
                post.getCommentCount(),
                post.getReactionCount(),
                post.getViewCount(),
                false,
                post.getLastActivityAt(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private String authorName(Long authorId) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, authorId));
        if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        User user = userMapper.selectById(authorId);
        return user == null ? "" : user.getUsername();
    }
}
