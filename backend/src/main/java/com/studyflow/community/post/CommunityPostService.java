package com.studyflow.community.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.member.UserProfile;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.community.post.dto.CommunityPostRequest;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.community.reaction.CommunityReactionService;
import com.studyflow.community.topic.CommunityTopic;
import com.studyflow.community.topic.CommunityTopicMapper;
import com.studyflow.media.MediaService;
import com.studyflow.media.dto.MediaAttachmentResponse;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.studyflow.community.member.CommunityMemberService.STATUS_ACTIVE;

@Service
public class CommunityPostService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String CONTENT_FORMAT_TEXT = "TEXT";
    private static final String VISIBILITY_CIRCLE = "CIRCLE";

    private final CommunityPostMapper communityPostMapper;
    private final CommunityTopicMapper communityTopicMapper;
    private final CommunityMemberService communityMemberService;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final CommunityReactionService communityReactionService;
    private final MediaService mediaService;

    public CommunityPostService(
            CommunityPostMapper communityPostMapper,
            CommunityTopicMapper communityTopicMapper,
            CommunityMemberService communityMemberService,
            UserProfileMapper userProfileMapper,
            UserMapper userMapper,
            CommunityReactionService communityReactionService,
            MediaService mediaService
    ) {
        this.communityPostMapper = communityPostMapper;
        this.communityTopicMapper = communityTopicMapper;
        this.communityMemberService = communityMemberService;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
        this.communityReactionService = communityReactionService;
        this.mediaService = mediaService;
    }

    public List<CommunityPostResponse> listFeed(Long userId) {
        Circle circle = communityMemberService.requireReadableDefaultMember(userId);
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getCircleId, circle.getId())
                        .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                        .orderByDesc(CommunityPost::getPinned)
                        .orderByDesc(CommunityPost::getLastActivityAt)
                        .orderByDesc(CommunityPost::getCreatedAt));
        return toResponses(posts, userId);
    }

    public CommunityPostResponse getPost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireReadableDefaultMember(userId);
        CommunityPost post = requirePublishedPost(circle.getId(), postId);
        return toResponse(post, userId);
    }

    public CommunityPost requirePublishedPost(Long circleId, Long postId) {
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circleId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED));
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        return post;
    }

    public void incrementCommentCount(Long postId, LocalDateTime now) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("comment_count = comment_count + 1")
                .set(CommunityPost::getLastActivityAt, now)
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(404, "帖子不存在");
        }
    }

    public void decrementCommentCount(Long postId, LocalDateTime now) {
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("comment_count = CASE WHEN comment_count > 0 THEN comment_count - 1 ELSE 0 END")
                .set(CommunityPost::getUpdatedAt, now));
    }

    @Transactional
    public CommunityPostResponse createPost(Long userId, CommunityPostRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
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
        if (post.getTopicId() != null) {
            communityTopicMapper.incrementPostCount(post.getTopicId());
        }
        mediaService.replacePostMedia(userId, post.getId(), request.mediaFileIds(), now);
        return toResponse(post, userId);
    }

    @Transactional
    public CommunityPostResponse updatePost(Long userId, Long postId, CommunityPostRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requireOwnedPost(circle.getId(), userId, postId);
        CommunityTopic topic = findActiveTopic(circle.getId(), request.topicId());
        Long previousTopicId = post.getTopicId();
        Long nextTopicId = topic == null ? null : topic.getId();
        LocalDateTime now = LocalDateTime.now();

        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, post.getId())
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getAuthorId, userId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .set(CommunityPost::getTopicId, nextTopicId)
                .set(CommunityPost::getTitle, request.title())
                .set(CommunityPost::getContent, request.content())
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Post status changed");
        }
        updateTopicCounts(previousTopicId, nextTopicId);
        post.setTopicId(nextTopicId);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setUpdatedAt(now);
        mediaService.replacePostMedia(userId, post.getId(), request.mediaFileIds(), now);
        return toResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requireOwnedPost(circle.getId(), userId, postId);
        LocalDateTime now = LocalDateTime.now();
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, post.getId())
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getAuthorId, userId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .set(CommunityPost::getStatus, STATUS_DELETED)
                .set(CommunityPost::getDeletedAt, now)
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Post status changed");
        }
        if (post.getTopicId() != null) {
            communityTopicMapper.decrementPostCount(post.getTopicId());
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

    private void updateTopicCounts(Long previousTopicId, Long nextTopicId) {
        if (Objects.equals(previousTopicId, nextTopicId)) {
            return;
        }
        if (previousTopicId != null) {
            communityTopicMapper.decrementPostCount(previousTopicId);
        }
        if (nextTopicId != null) {
            communityTopicMapper.incrementPostCount(nextTopicId);
        }
    }

    private List<CommunityPostResponse> toResponses(List<CommunityPost> posts, Long userId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = posts.stream()
                .map(CommunityPost::getAuthorId)
                .collect(Collectors.toSet());
        Set<Long> postIds = posts.stream()
                .map(CommunityPost::getId)
                .collect(Collectors.toSet());
        Set<Long> topicIds = posts.stream()
                .map(CommunityPost::getTopicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> authorNames = authorNames(authorIds);
        Map<Long, CommunityTopic> topics = topics(topicIds);
        Set<Long> likedPostIds = communityReactionService.likedPostIds(userId, postIds);
        Map<Long, List<MediaAttachmentResponse>> mediaByPostId = mediaService.attachmentsByPostIds(postIds);
        return posts.stream()
                .map(post -> toResponse(post, authorNames, topics, likedPostIds, mediaByPostId))
                .toList();
    }

    private CommunityPostResponse toResponse(CommunityPost post, Long userId) {
        return toResponses(List.of(post), userId).get(0);
    }

    private CommunityPostResponse toResponse(
            CommunityPost post,
            Map<Long, String> authorNames,
            Map<Long, CommunityTopic> topics,
            Set<Long> likedPostIds,
            Map<Long, List<MediaAttachmentResponse>> mediaByPostId
    ) {
        CommunityTopic topic = post.getTopicId() == null ? null : topics.get(post.getTopicId());
        return new CommunityPostResponse(
                post.getId(),
                post.getCircleId(),
                post.getAuthorId(),
                authorNames.getOrDefault(post.getAuthorId(), ""),
                post.getTopicId(),
                topic == null ? null : topic.getName(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getPinned(),
                post.getCommentCount(),
                post.getReactionCount(),
                post.getViewCount(),
                likedPostIds.contains(post.getId()),
                mediaByPostId.getOrDefault(post.getId(), Collections.emptyList()),
                post.getLastActivityAt(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private Map<Long, String> authorNames(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, authorIds))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getUserId, authorIds))
                .forEach(profile -> {
                    if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
                        names.put(profile.getUserId(), profile.getDisplayName());
                    }
                });
        return names;
    }

    private Map<Long, CommunityTopic> topics(Set<Long> topicIds) {
        if (topicIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return communityTopicMapper.selectList(new LambdaQueryWrapper<CommunityTopic>()
                        .in(CommunityTopic::getId, topicIds))
                .stream()
                .collect(Collectors.toMap(CommunityTopic::getId, Function.identity()));
    }
}
