package com.studyflow.community.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.collection.CommunityCollection;
import com.studyflow.community.collection.CommunityCollectionMapper;
import com.studyflow.community.danmaku.CommunityDanmaku;
import com.studyflow.community.danmaku.CommunityDanmakuMapper;
import com.studyflow.community.favorite.CommunityFavoriteService;
import com.studyflow.community.follow.UserFollow;
import com.studyflow.community.follow.UserFollowMapper;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.member.UserProfile;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.community.post.dto.CommunityCollectionItemResponse;
import com.studyflow.community.post.dto.CommunityCollectionItemsPageResponse;
import com.studyflow.community.post.dto.CommunityCollectionSummaryResponse;
import com.studyflow.community.post.dto.CommunityPostCollectionRequest;
import com.studyflow.community.post.dto.CommunityPostCollectionResponse;
import com.studyflow.community.post.dto.CommunityPostRequest;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.community.ranking.CommunityPostRankingService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityPostService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String CONTENT_TYPE_ARTICLE = "ARTICLE";
    private static final String CONTENT_TYPE_VIDEO = "VIDEO";
    private static final String CONTENT_FORMAT_TEXT = "TEXT";
    private static final String VISIBILITY_CIRCLE = "CIRCLE";

    private final CommunityPostMapper communityPostMapper;
    private final CommunityCollectionMapper communityCollectionMapper;
    private final CommunityTopicMapper communityTopicMapper;
    private final CommunityMemberService communityMemberService;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final CommunityReactionService communityReactionService;
    private final MediaService mediaService;
    private final CommunityDanmakuMapper communityDanmakuMapper;
    private final CommunityFavoriteService communityFavoriteService;
    private final CommunityPostCacheService communityPostCacheService;
    private final CommunityPostCounterService communityPostCounterService;
    private final CommunityPostRankingService communityPostRankingService;
    private final UserFollowMapper userFollowMapper;

    public CommunityPostService(
            CommunityPostMapper communityPostMapper,
            CommunityCollectionMapper communityCollectionMapper,
            CommunityTopicMapper communityTopicMapper,
            CommunityMemberService communityMemberService,
            UserProfileMapper userProfileMapper,
            UserMapper userMapper,
            CommunityReactionService communityReactionService,
            MediaService mediaService,
            CommunityDanmakuMapper communityDanmakuMapper,
            CommunityFavoriteService communityFavoriteService,
            CommunityPostCacheService communityPostCacheService,
            CommunityPostCounterService communityPostCounterService,
            CommunityPostRankingService communityPostRankingService,
            UserFollowMapper userFollowMapper
    ) {
        this.communityPostMapper = communityPostMapper;
        this.communityCollectionMapper = communityCollectionMapper;
        this.communityTopicMapper = communityTopicMapper;
        this.communityMemberService = communityMemberService;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
        this.communityReactionService = communityReactionService;
        this.mediaService = mediaService;
        this.communityDanmakuMapper = communityDanmakuMapper;
        this.communityFavoriteService = communityFavoriteService;
        this.communityPostCacheService = communityPostCacheService;
        this.communityPostCounterService = communityPostCounterService;
        this.communityPostRankingService = communityPostRankingService;
        this.userFollowMapper = userFollowMapper;
    }

    public List<CommunityPostResponse> listFeed(Long userId) {
        if (userId == null) {
            return communityPostCacheService.getFeed()
                    .map(posts -> withDynamicFields(posts, null))
                    .orElseGet(() -> {
                        List<CommunityPostResponse> posts = listFeedFromDatabase(null);
                        communityPostCacheService.cacheFeed(posts);
                        return posts;
                    });
        }
        return communityPostCacheService.getFeed()
                .map(posts -> withDynamicFields(posts, userId))
                .orElseGet(() -> {
                    List<CommunityPostResponse> posts = listFeedFromDatabase(null);
                    communityPostCacheService.cacheFeed(posts);
                    return personalizeResponses(posts, userId);
                });
    }

    private List<CommunityPostResponse> listFeedFromDatabase(Long userId) {
        Circle circle = communityMemberService.getDefaultCircle();
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getCircleId, circle.getId())
                        .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                        .orderByDesc(CommunityPost::getPinned)
                        .orderByDesc(CommunityPost::getLastActivityAt)
                        .orderByDesc(CommunityPost::getCreatedAt));
        return toResponses(posts, userId);
    }

    public List<CommunityPostResponse> searchPosts(Long userId, String keyword) {
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        if (normalizedKeyword == null) {
            return Collections.emptyList();
        }

        Circle circle = communityMemberService.getDefaultCircle();
        Set<Long> authorIds = searchAuthorIds(normalizedKeyword);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .and(query -> {
                    query.like(CommunityPost::getTitle, normalizedKeyword)
                            .or()
                            .like(CommunityPost::getContent, normalizedKeyword)
                            .or()
                            .like(CommunityPost::getTopicName, normalizedKeyword);
                    if (!authorIds.isEmpty()) {
                        query.or().in(CommunityPost::getAuthorId, authorIds);
                    }
                })
                .orderByDesc(CommunityPost::getPinned)
                .orderByDesc(CommunityPost::getLastActivityAt)
                .orderByDesc(CommunityPost::getId)
                .last("LIMIT 50");
        return toResponses(communityPostMapper.selectList(wrapper), userId);
    }

    public List<CommunityPostResponse> listHotRanking(Long userId) {
        List<Long> rankingIds = communityPostRankingService.getRankingIds(1, 20);
        if (!rankingIds.isEmpty()) {
            return listPublishedPostsByIds(userId, rankingIds);
        }
        return listHotRankingFromDatabase(userId);
    }

    private List<CommunityPostResponse> listHotRankingFromDatabase(Long userId) {
        Circle circle = communityMemberService.getDefaultCircle();
        QueryWrapper<CommunityPost> wrapper = new QueryWrapper<CommunityPost>()
                .eq("circle_id", circle.getId())
                .eq("status", STATUS_PUBLISHED)
                .last("""
                        ORDER BY pinned DESC,
                        (
                          COALESCE(view_count, 0)
                          + COALESCE(reaction_count, 0) * 3
                          + COALESCE(comment_count, 0) * 2
                          + COALESCE(pig_count, 0) * 5
                          + COALESCE(favorite_count, 0) * 4
                        ) DESC,
                        last_activity_at DESC,
                        id DESC
                        LIMIT 20
                        """);
        return toResponses(communityPostMapper.selectList(wrapper), userId);
    }

    public List<CommunityPostResponse> listFollowingFeed(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Circle circle = communityMemberService.getDefaultCircle();
        List<Long> followingIds = userFollowMapper.selectList(
                        new LambdaQueryWrapper<UserFollow>()
                                .eq(UserFollow::getFollowerId, userId)
                                .select(UserFollow::getFollowingId))
                .stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .in(CommunityPost::getAuthorId, followingIds)
                .orderByDesc(CommunityPost::getPinned)
                .orderByDesc(CommunityPost::getLastActivityAt)
                .orderByDesc(CommunityPost::getId));
        return toResponses(posts, userId);
    }

    public List<CommunityPostResponse> listRelatedPosts(Long userId, Long postId) {
        Circle circle = communityMemberService.getDefaultCircle();
        CommunityPost current = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED));
        if (current == null) {
            return Collections.emptyList();
        }

        List<CommunityPost> related = new ArrayList<>();
        String topicName = current.getTopicName();
        if (topicName != null && !topicName.isBlank()) {
            related = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                    .eq(CommunityPost::getCircleId, circle.getId())
                    .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                    .eq(CommunityPost::getTopicName, topicName)
                    .ne(CommunityPost::getId, postId)
                    .orderByDesc(CommunityPost::getPinned)
                    .orderByDesc(CommunityPost::getLastActivityAt)
                    .orderByDesc(CommunityPost::getId)
                    .last("LIMIT 8"));
        }

        if (related.size() < 8) {
            Set<Long> existingIds = related.stream().map(CommunityPost::getId).collect(Collectors.toSet());
            existingIds.add(postId);
            List<CommunityPost> latest = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                    .eq(CommunityPost::getCircleId, circle.getId())
                    .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                    .notIn(!existingIds.isEmpty(), CommunityPost::getId, existingIds)
                    .orderByDesc(CommunityPost::getPinned)
                    .orderByDesc(CommunityPost::getLastActivityAt)
                    .orderByDesc(CommunityPost::getId)
                    .last("LIMIT " + (8 - related.size())));
            related.addAll(latest);
        }

        return toResponses(related, userId);
    }

    public CommunityPostResponse getPost(Long userId, Long postId) {
        if (userId == null) {
            return communityPostCacheService.getPostDetail(postId)
                    .map(post -> withDynamicFields(List.of(post), null).get(0))
                    .orElseGet(() -> {
                        CommunityPostResponse post = getPostFromDatabase(null, postId);
                        communityPostCacheService.cachePostDetail(postId, post);
                        return post;
                    });
        }
        return communityPostCacheService.getPostDetail(postId)
                .map(post -> withDynamicFields(List.of(post), userId).get(0))
                .orElseGet(() -> {
                    CommunityPostResponse post = getPostFromDatabase(null, postId);
                    communityPostCacheService.cachePostDetail(postId, post);
                    return personalizeResponse(post, userId);
                });
    }

    private CommunityPostResponse getPostFromDatabase(Long userId, Long postId) {
        Circle circle = communityMemberService.getDefaultCircle();
        CommunityPost post = requirePublishedPost(circle.getId(), postId);
        return toResponse(post, userId);
    }

    public List<CommunityPostResponse> listPublishedPostsByIds(Long userId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        Circle circle = communityMemberService.getDefaultCircle();
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .in(CommunityPost::getId, postIds));
        Map<Long, Integer> orderByPostId = new HashMap<>();
        for (int index = 0; index < postIds.size(); index++) {
            orderByPostId.put(postIds.get(index), index);
        }
        posts.sort((left, right) -> Integer.compare(
                orderByPostId.getOrDefault(left.getId(), Integer.MAX_VALUE),
                orderByPostId.getOrDefault(right.getId(), Integer.MAX_VALUE)
        ));
        return toResponses(posts, userId);
    }

    public List<CommunityPostResponse> listFavoritePosts(Long userId) {
        List<Long> favoritePostIds = communityFavoriteService.favoritePostIdsByUser(userId);
        if (favoritePostIds.isEmpty()) {
            return Collections.emptyList();
        }

        Circle circle = communityMemberService.getDefaultCircle();
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .in(CommunityPost::getId, favoritePostIds));
        Map<Long, Integer> orderByFavoriteTime = new HashMap<>();
        for (int index = 0; index < favoritePostIds.size(); index++) {
            orderByFavoriteTime.put(favoritePostIds.get(index), index);
        }
        posts.sort((left, right) -> Integer.compare(
                orderByFavoriteTime.getOrDefault(left.getId(), Integer.MAX_VALUE),
                orderByFavoriteTime.getOrDefault(right.getId(), Integer.MAX_VALUE)
        ));
        return toResponses(posts, userId);
    }

    public List<CommunityPostResponse> listMySubmissions(Long userId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getAuthorId, userId)
                .in(CommunityPost::getStatus, List.of(STATUS_PENDING_REVIEW, STATUS_REJECTED, STATUS_PUBLISHED))
                .orderByDesc(CommunityPost::getCreatedAt)
                .orderByDesc(CommunityPost::getId));
        return toResponses(posts, userId);
    }

    public List<CommunityCollectionSummaryResponse> listMyCollections(Long userId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        List<CommunityCollection> collections = communityCollectionMapper.selectList(new LambdaQueryWrapper<CommunityCollection>()
                .eq(CommunityCollection::getCircleId, circle.getId())
                .eq(CommunityCollection::getAuthorId, userId)
                .eq(CommunityCollection::getStatus, STATUS_ACTIVE)
                .orderByDesc(CommunityCollection::getUpdatedAt)
                .orderByDesc(CommunityCollection::getId));
        if (collections.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> collectionIds = collections.stream()
                .map(CommunityCollection::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> postCounts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .select(CommunityPost::getId, CommunityPost::getCollectionId)
                        .in(CommunityPost::getCollectionId, collectionIds)
                        .eq(CommunityPost::getStatus, STATUS_PUBLISHED))
                .stream()
                .collect(Collectors.groupingBy(CommunityPost::getCollectionId, Collectors.summingInt(post -> 1)));

        return collections.stream()
                .map(collection -> new CommunityCollectionSummaryResponse(
                        collection.getId(),
                        collection.getTitle(),
                        collection.getDescription(),
                        postCounts.getOrDefault(collection.getId(), 0),
                        collection.getUpdatedAt()
                ))
                .toList();
    }

    public CommunityCollectionItemsPageResponse listCollectionItems(Long collectionId, Integer page, Integer pageSize) {
        Circle circle = communityMemberService.getDefaultCircle();
        CommunityCollection collection = communityCollectionMapper.selectOne(new LambdaQueryWrapper<CommunityCollection>()
                .eq(CommunityCollection::getId, collectionId)
                .eq(CommunityCollection::getCircleId, circle.getId())
                .eq(CommunityCollection::getStatus, STATUS_ACTIVE));
        if (collection == null) {
            throw new BusinessException(404, "专栏不存在");
        }

        int safePage = Math.max(1, page == null ? 1 : page);
        int safePageSize = Math.min(50, Math.max(1, pageSize == null ? 10 : pageSize));
        long total = communityPostMapper.selectCount(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCollectionId, collectionId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED));
        List<CommunityCollectionItemResponse> items = collectionItems(collectionId, safePage, safePageSize);

        return new CommunityCollectionItemsPageResponse(
                collectionId,
                safePage,
                safePageSize,
                total,
                (long) safePage * safePageSize < total,
                items
        );
    }

    public List<CommunityPostResponse> listPendingReviewSubmissions(Long currentUserId, Long circleId) {
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circleId)
                .eq(CommunityPost::getStatus, STATUS_PENDING_REVIEW)
                .orderByAsc(CommunityPost::getCreatedAt)
                .orderByAsc(CommunityPost::getId));
        return toResponses(posts, currentUserId);
    }

    public List<CommunityPostResponse> listAuthorPosts(Long currentUserId, Long authorId) {
        Circle circle = communityMemberService.getDefaultCircle();
        List<CommunityPost> posts = communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getAuthorId, authorId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .orderByDesc(CommunityPost::getCreatedAt)
                .orderByDesc(CommunityPost::getId));
        return toResponses(posts, currentUserId);
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
        communityPostCacheService.evictFeedAndPost(postId);
        communityPostRankingService.updateRanking(postId);
    }

    public void decrementCommentCount(Long postId, LocalDateTime now) {
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("comment_count = CASE WHEN comment_count > 0 THEN comment_count - 1 ELSE 0 END")
                .set(CommunityPost::getUpdatedAt, now));
        communityPostCacheService.evictFeedAndPost(postId);
        communityPostRankingService.updateRanking(postId);
    }

    public void incrementViewCount(Long postId, LocalDateTime now) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("view_count = view_count + 1")
                .set(CommunityPost::getUpdatedAt, now));
        if (updated > 0) {
            communityPostCounterService.refreshPostCounter(postId);
            communityPostRankingService.updateRanking(postId);
        }
    }

    private Set<Long> searchAuthorIds(String keyword) {
        Set<Long> authorIds = new HashSet<>();
        userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword))
                .forEach(user -> authorIds.add(user.getId()));
        userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .like(UserProfile::getDisplayName, keyword))
                .forEach(profile -> authorIds.add(profile.getUserId()));
        return authorIds;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public CommunityPostResponse createPost(Long userId, CommunityPostRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityTopic topic = findActiveTopic(circle.getId(), request.topicId());
        LocalDateTime now = LocalDateTime.now();
        String contentType = mediaService.resolvePostContentType(userId, request.mediaFileIds());
        CommunityCollection collection = resolveCollectionForPost(
                circle.getId(),
                userId,
                null,
                request.collectionEnabled(),
                request.collectionId(),
                request.collectionTitle(),
                request.collectionDescription(),
                now
        );

        CommunityPost post = new CommunityPost();
        post.setCircleId(circle.getId());
        post.setAuthorId(userId);
        post.setTopicId(topic == null ? null : topic.getId());
        post.setTopicName(resolveTopicName(topic, request.topicName()));
        post.setCollectionId(collection == null ? null : collection.getId());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setVideoCoverMediaFileId(request.videoCoverMediaFileId());
        post.setContentType(contentType);
        post.setContentFormat(CONTENT_FORMAT_TEXT);
        post.setVisibility(VISIBILITY_CIRCLE);
        post.setStatus(initialStatus(contentType));
        post.setPinned(false);
        post.setCommentCount(0);
        post.setReactionCount(0);
        post.setPigCount(0);
        post.setFavoriteCount(0);
        post.setViewCount(0);
        post.setLastActivityAt(now);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        communityPostMapper.insert(post);
        if (STATUS_PUBLISHED.equals(post.getStatus()) && post.getTopicId() != null) {
            communityTopicMapper.incrementPostCount(post.getTopicId());
        }
        mediaService.replacePostMedia(userId, post.getId(), request.mediaFileIds(), request.videoCoverMediaFileId(), now);
        if (STATUS_PUBLISHED.equals(post.getStatus())) {
            evictCollectionCaches(null, post.getCollectionId(), post.getId());
        }
        return toResponse(post, userId);
    }

    @Transactional
    public CommunityPostResponse updatePost(Long userId, Long postId, CommunityPostRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requireOwnedPost(circle.getId(), userId, postId);
        CommunityTopic topic = findActiveTopic(circle.getId(), request.topicId());
        Long previousTopicId = post.getTopicId();
        Long previousCollectionId = post.getCollectionId();
        Long nextTopicId = topic == null ? null : topic.getId();
        String nextTopicName = resolveTopicName(topic, request.topicName());
        LocalDateTime now = LocalDateTime.now();
        String nextContentType = mediaService.resolvePostContentType(userId, request.mediaFileIds());
        CommunityCollection collection = resolveCollectionForPost(
                circle.getId(),
                userId,
                previousCollectionId,
                request.collectionEnabled(),
                request.collectionId(),
                request.collectionTitle(),
                request.collectionDescription(),
                now
        );
        Long nextCollectionId = collection == null ? null : collection.getId();

        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, post.getId())
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getAuthorId, userId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .set(CommunityPost::getTopicId, nextTopicId)
                .set(CommunityPost::getTopicName, nextTopicName)
                .set(CommunityPost::getCollectionId, nextCollectionId)
                .set(CommunityPost::getTitle, request.title())
                .set(CommunityPost::getContent, request.content())
                .set(CommunityPost::getVideoCoverMediaFileId, request.videoCoverMediaFileId())
                .set(CommunityPost::getContentType, nextContentType)
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Post status changed");
        }
        updateTopicCounts(previousTopicId, nextTopicId);
        post.setTopicId(nextTopicId);
        post.setTopicName(nextTopicName);
        post.setCollectionId(nextCollectionId);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setVideoCoverMediaFileId(request.videoCoverMediaFileId());
        post.setContentType(nextContentType);
        post.setUpdatedAt(now);
        mediaService.replacePostMedia(userId, post.getId(), request.mediaFileIds(), request.videoCoverMediaFileId(), now);
        evictCollectionCaches(previousCollectionId, nextCollectionId, post.getId());
        return toResponse(post, userId);
    }

    @Transactional
    public CommunityPostResponse updatePostCollection(Long userId, Long postId, CommunityPostCollectionRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requireOwnedPost(circle.getId(), userId, postId);
        Long previousCollectionId = post.getCollectionId();
        LocalDateTime now = LocalDateTime.now();
        CommunityCollection collection = resolveCollectionForPost(
                circle.getId(),
                userId,
                previousCollectionId,
                request.enabled(),
                request.collectionId(),
                request.title(),
                request.description(),
                now
        );
        Long nextCollectionId = collection == null ? null : collection.getId();

        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, post.getId())
                .eq(CommunityPost::getCircleId, circle.getId())
                .eq(CommunityPost::getAuthorId, userId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .set(CommunityPost::getCollectionId, nextCollectionId)
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "Post status changed");
        }

        post.setCollectionId(nextCollectionId);
        post.setUpdatedAt(now);
        evictCollectionCaches(previousCollectionId, nextCollectionId, post.getId());
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
        evictCollectionCaches(post.getCollectionId(), null, post.getId());
        communityPostCounterService.evictPostCounter(post.getId());
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

    private String resolveTopicName(CommunityTopic topic, String manualTopicName) {
        String normalizedManualTopicName = normalizeTopicName(manualTopicName);
        if (normalizedManualTopicName != null) {
            return normalizedManualTopicName;
        }
        return topic == null ? null : topic.getName();
    }

    private String normalizeTopicName(String topicName) {
        if (topicName == null) {
            return null;
        }
        String trimmedTopicName = topicName.trim();
        return trimmedTopicName.isEmpty() ? null : trimmedTopicName;
    }

    private String initialStatus(String contentType) {
        return CONTENT_TYPE_VIDEO.equals(contentType) ? STATUS_PENDING_REVIEW : STATUS_PUBLISHED;
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

    private CommunityCollection resolveCollectionForPost(
            Long circleId,
            Long userId,
            Long previousCollectionId,
            Boolean enabled,
            Long requestedCollectionId,
            String title,
            String description,
            LocalDateTime now
    ) {
        if (Boolean.FALSE.equals(enabled)) {
            return null;
        }
        if (enabled == null && requestedCollectionId == null && title == null && description == null) {
            return previousCollectionId == null ? null : requireOwnedCollection(circleId, userId, previousCollectionId);
        }
        if (!Boolean.TRUE.equals(enabled)) {
            return previousCollectionId == null ? null : requireOwnedCollection(circleId, userId, previousCollectionId);
        }
        if (requestedCollectionId != null) {
            return requireOwnedCollection(circleId, userId, requestedCollectionId);
        }

        String normalizedTitle = normalizeCollectionTitle(title);
        String normalizedDescription = normalizeCollectionDescription(description);
        if (normalizedTitle == null) {
            if (previousCollectionId != null) {
                CommunityCollection previous = requireOwnedCollection(circleId, userId, previousCollectionId);
                updateCollectionDescription(previous, normalizedDescription, now);
                return previous;
            }
            throw new BusinessException(400, "专栏标题不能为空");
        }

        CommunityCollection existing = findOwnedCollectionByTitle(circleId, userId, normalizedTitle);
        if (existing != null && !Objects.equals(existing.getId(), previousCollectionId)) {
            return existing;
        }
        if (previousCollectionId != null) {
            CommunityCollection previous = requireOwnedCollection(circleId, userId, previousCollectionId);
            updateCollection(previous, normalizedTitle, normalizedDescription, now);
            return previous;
        }

        CommunityCollection collection = new CommunityCollection();
        collection.setCircleId(circleId);
        collection.setAuthorId(userId);
        collection.setTitle(normalizedTitle);
        collection.setDescription(normalizedDescription);
        collection.setStatus(STATUS_ACTIVE);
        collection.setCreatedAt(now);
        collection.setUpdatedAt(now);
        communityCollectionMapper.insert(collection);
        return collection;
    }

    private CommunityCollection requireOwnedCollection(Long circleId, Long userId, Long collectionId) {
        CommunityCollection collection = communityCollectionMapper.selectOne(new LambdaQueryWrapper<CommunityCollection>()
                .eq(CommunityCollection::getId, collectionId)
                .eq(CommunityCollection::getCircleId, circleId)
                .eq(CommunityCollection::getAuthorId, userId)
                .eq(CommunityCollection::getStatus, STATUS_ACTIVE));
        if (collection == null) {
            throw new BusinessException(404, "专栏不存在");
        }
        return collection;
    }

    private CommunityCollection findOwnedCollectionByTitle(Long circleId, Long userId, String title) {
        return communityCollectionMapper.selectOne(new LambdaQueryWrapper<CommunityCollection>()
                .eq(CommunityCollection::getCircleId, circleId)
                .eq(CommunityCollection::getAuthorId, userId)
                .eq(CommunityCollection::getTitle, title)
                .eq(CommunityCollection::getStatus, STATUS_ACTIVE));
    }

    private void updateCollection(CommunityCollection collection, String title, String description, LocalDateTime now) {
        if (Objects.equals(collection.getTitle(), title) && Objects.equals(collection.getDescription(), description)) {
            return;
        }
        collection.setTitle(title);
        collection.setDescription(description);
        collection.setUpdatedAt(now);
        communityCollectionMapper.updateById(collection);
    }

    private void updateCollectionDescription(CommunityCollection collection, String description, LocalDateTime now) {
        if (Objects.equals(collection.getDescription(), description)) {
            return;
        }
        collection.setDescription(description);
        collection.setUpdatedAt(now);
        communityCollectionMapper.updateById(collection);
    }

    private String normalizeCollectionTitle(String title) {
        if (title == null) {
            return null;
        }
        String trimmed = title.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCollectionDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void evictCollectionCaches(Long previousCollectionId, Long nextCollectionId, Long fallbackPostId) {
        Set<Long> postIds = new HashSet<>();
        postIds.add(fallbackPostId);
        Set<Long> collectionIds = new HashSet<>();
        if (previousCollectionId != null) {
            collectionIds.add(previousCollectionId);
        }
        if (nextCollectionId != null) {
            collectionIds.add(nextCollectionId);
        }
        if (!collectionIds.isEmpty()) {
            communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                            .select(CommunityPost::getId)
                            .in(CommunityPost::getCollectionId, collectionIds))
                    .forEach(post -> postIds.add(post.getId()));
        }
        communityPostCacheService.evictFeed();
        postIds.stream()
                .filter(Objects::nonNull)
                .forEach(communityPostCacheService::evictPost);
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
        Set<Long> collectionIds = posts.stream()
                .map(CommunityPost::getCollectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, AuthorDisplay> authorDisplays = authorDisplays(authorIds);
        Map<Long, CommunityTopic> topics = topics(topicIds);
        Map<Long, CommunityPostCollectionResponse> collections = collectionResponses(collectionIds);
        Set<Long> likedPostIds = communityReactionService.likedPostIds(userId, postIds);
        Set<Long> piggedPostIds = communityReactionService.piggedPostIds(userId, postIds);
        Set<Long> favoritedPostIds = communityFavoriteService.favoritedPostIds(userId, postIds);
        Map<Long, Integer> danmakuCounts = publishedDanmakuCounts(postIds);
        Map<Long, Long> videoCoverMediaFileIds = posts.stream()
                .filter(post -> post.getVideoCoverMediaFileId() != null)
                .collect(Collectors.toMap(CommunityPost::getId, CommunityPost::getVideoCoverMediaFileId));
        Map<Long, List<MediaAttachmentResponse>> mediaByPostId =
                mediaService.attachmentsByPostIds(postIds, videoCoverMediaFileIds);
        return posts.stream()
                .map(post -> toResponse(
                        post,
                        authorDisplays,
                        topics,
                        likedPostIds,
                        piggedPostIds,
                        favoritedPostIds,
                        danmakuCounts,
                        collections,
                        mediaByPostId
                ))
                .toList();
    }

    private List<CommunityPostResponse> withDynamicFields(List<CommunityPostResponse> posts, Long userId) {
        return personalizeResponses(communityPostCounterService.applyCounters(posts), userId);
    }

    private List<CommunityPostResponse> personalizeResponses(List<CommunityPostResponse> posts, Long userId) {
        if (posts.isEmpty() || userId == null) {
            return posts;
        }
        Set<Long> postIds = posts.stream()
                .map(CommunityPostResponse::id)
                .collect(Collectors.toSet());
        Set<Long> likedPostIds = communityReactionService.likedPostIds(userId, postIds);
        Set<Long> piggedPostIds = communityReactionService.piggedPostIds(userId, postIds);
        Set<Long> favoritedPostIds = communityFavoriteService.favoritedPostIds(userId, postIds);
        return posts.stream()
                .map(post -> withViewerState(
                        post,
                        likedPostIds.contains(post.id()),
                        piggedPostIds.contains(post.id()),
                        favoritedPostIds.contains(post.id())
                ))
                .toList();
    }

    private CommunityPostResponse personalizeResponse(CommunityPostResponse post, Long userId) {
        return personalizeResponses(List.of(post), userId).get(0);
    }

    private CommunityPostResponse withViewerState(
            CommunityPostResponse post,
            boolean liked,
            boolean pigged,
            boolean favorited
    ) {
        return new CommunityPostResponse(
                post.id(),
                post.circleId(),
                post.authorId(),
                post.authorName(),
                post.authorAvatarUrl(),
                post.topicId(),
                post.topicName(),
                post.title(),
                post.content(),
                post.contentType(),
                post.status(),
                post.reviewedBy(),
                post.reviewedAt(),
                post.reviewReason(),
                post.pinned(),
                post.commentCount(),
                post.danmakuCount(),
                post.reactionCount(),
                post.pigCount(),
                post.favoriteCount(),
                post.viewCount(),
                liked,
                pigged,
                favorited,
                post.media(),
                post.collection(),
                post.lastActivityAt(),
                post.createdAt(),
                post.updatedAt()
        );
    }

    private CommunityPostResponse toResponse(CommunityPost post, Long userId) {
        return toResponses(List.of(post), userId).get(0);
    }

    private CommunityPostResponse toResponse(
            CommunityPost post,
            Map<Long, AuthorDisplay> authorDisplays,
            Map<Long, CommunityTopic> topics,
            Set<Long> likedPostIds,
            Set<Long> piggedPostIds,
            Set<Long> favoritedPostIds,
            Map<Long, Integer> danmakuCounts,
            Map<Long, CommunityPostCollectionResponse> collections,
            Map<Long, List<MediaAttachmentResponse>> mediaByPostId
    ) {
        CommunityTopic topic = post.getTopicId() == null ? null : topics.get(post.getTopicId());
        String topicName = post.getTopicName() != null ? post.getTopicName() : topic == null ? null : topic.getName();
        AuthorDisplay author = authorDisplays.getOrDefault(post.getAuthorId(), new AuthorDisplay("", null));
        return new CommunityPostResponse(
                post.getId(),
                post.getCircleId(),
                post.getAuthorId(),
                author.name(),
                author.avatarUrl(),
                post.getTopicId(),
                topicName,
                post.getTitle(),
                post.getContent(),
                post.getContentType() == null ? CONTENT_TYPE_ARTICLE : post.getContentType(),
                post.getStatus(),
                post.getReviewedBy(),
                post.getReviewedAt(),
                post.getReviewReason(),
                post.getPinned(),
                post.getCommentCount(),
                danmakuCounts.getOrDefault(post.getId(), 0),
                post.getReactionCount(),
                post.getPigCount() == null ? 0 : post.getPigCount(),
                post.getFavoriteCount() == null ? 0 : post.getFavoriteCount(),
                post.getViewCount(),
                likedPostIds.contains(post.getId()),
                piggedPostIds.contains(post.getId()),
                favoritedPostIds.contains(post.getId()),
                mediaByPostId.getOrDefault(post.getId(), Collections.emptyList()),
                post.getCollectionId() == null ? null : collections.get(post.getCollectionId()),
                post.getLastActivityAt(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private Map<Long, CommunityPostCollectionResponse> collectionResponses(Set<Long> collectionIds) {
        if (collectionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CommunityCollection> collections = communityCollectionMapper.selectList(
                        new LambdaQueryWrapper<CommunityCollection>()
                                .in(CommunityCollection::getId, collectionIds)
                                .eq(CommunityCollection::getStatus, STATUS_ACTIVE))
                .stream()
                .collect(Collectors.toMap(CommunityCollection::getId, Function.identity()));
        if (collections.isEmpty()) {
            return Collections.emptyMap();
        }

        return collections.values().stream()
                .collect(Collectors.toMap(
                        CommunityCollection::getId,
                        collection -> new CommunityPostCollectionResponse(
                                collection.getId(),
                                collection.getTitle(),
                                collection.getDescription(),
                                collectionItems(collection.getId(), 1, 10)
                        )
                ));
    }

    private List<CommunityCollectionItemResponse> collectionItems(Long collectionId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getCollectionId, collectionId)
                        .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                        .orderByAsc(CommunityPost::getCreatedAt)
                        .orderByAsc(CommunityPost::getId)
                        .last("LIMIT " + pageSize + " OFFSET " + offset))
                .stream()
                .map(post -> new CommunityCollectionItemResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getContentType() == null ? CONTENT_TYPE_ARTICLE : post.getContentType(),
                        post.getViewCount() == null ? 0 : post.getViewCount(),
                        post.getCreatedAt()
                ))
                .toList();
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

    private Map<Long, CommunityTopic> topics(Set<Long> topicIds) {
        if (topicIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return communityTopicMapper.selectList(new LambdaQueryWrapper<CommunityTopic>()
                        .in(CommunityTopic::getId, topicIds))
                .stream()
                .collect(Collectors.toMap(CommunityTopic::getId, Function.identity()));
    }

    private Map<Long, Integer> publishedDanmakuCounts(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return communityDanmakuMapper.selectList(new LambdaQueryWrapper<CommunityDanmaku>()
                        .in(CommunityDanmaku::getPostId, postIds)
                        .eq(CommunityDanmaku::getStatus, STATUS_PUBLISHED))
                .stream()
                .collect(Collectors.groupingBy(CommunityDanmaku::getPostId, Collectors.summingInt(item -> 1)));
    }
}
