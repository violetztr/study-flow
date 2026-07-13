package com.studyflow.community.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.infrastructure.redis.RedisCacheService;
import com.studyflow.infrastructure.redis.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityPostCounterService {
    private static final Logger log = LoggerFactory.getLogger(CommunityPostCounterService.class);
    private static final Duration COUNTER_TTL = Duration.ofHours(6);

    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;
    private final CommunityPostMapper communityPostMapper;

    public CommunityPostCounterService(
            RedisCacheService redisCacheService,
            ObjectMapper objectMapper,
            CommunityPostMapper communityPostMapper
    ) {
        this.redisCacheService = redisCacheService;
        this.objectMapper = objectMapper;
        this.communityPostMapper = communityPostMapper;
    }

    public List<CommunityPostResponse> applyCounters(List<CommunityPostResponse> posts) {
        if (posts.isEmpty()) {
            return posts;
        }
        Map<Long, CommunityPostCounters> countersByPostId = countersByPostIds(posts.stream()
                .map(CommunityPostResponse::id)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return posts.stream()
                .map(post -> applyCounters(post, countersByPostId.get(post.id())))
                .toList();
    }

    public CommunityPostResponse applyCounters(CommunityPostResponse post) {
        return applyCounters(post, countersByPostIds(List.of(post.id())).get(post.id()));
    }

    public void refreshPostCounter(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            evictPostCounter(postId);
            return;
        }
        writeCounter(postId, CommunityPostCounters.fromPost(post));
    }

    public void evictPostCounter(Long postId) {
        redisCacheService.delete(RedisKeys.postCounter(postId));
    }

    private Map<Long, CommunityPostCounters> countersByPostIds(Collection<Long> postIds) {
        Set<Long> uniquePostIds = postIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniquePostIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, CommunityPostCounters> result = new HashMap<>();
        List<Long> missingPostIds = new ArrayList<>();
        for (Long postId : uniquePostIds) {
            Optional<CommunityPostCounters> cached = readCounter(postId);
            if (cached.isPresent()) {
                result.put(postId, cached.get());
            } else {
                missingPostIds.add(postId);
            }
        }

        if (!missingPostIds.isEmpty()) {
            Map<Long, CommunityPostCounters> restored = restoreCountersFromDatabase(missingPostIds);
            result.putAll(restored);
            restored.forEach(this::writeCounter);
        }
        return result;
    }

    private Map<Long, CommunityPostCounters> restoreCountersFromDatabase(List<Long> postIds) {
        return communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .select(
                                CommunityPost::getId,
                                CommunityPost::getReactionCount,
                                CommunityPost::getPigCount,
                                CommunityPost::getFavoriteCount,
                                CommunityPost::getViewCount
                        )
                        .in(CommunityPost::getId, postIds))
                .stream()
                .collect(Collectors.toMap(
                        CommunityPost::getId,
                        CommunityPostCounters::fromPost,
                        (left, right) -> left
                ));
    }

    private Optional<CommunityPostCounters> readCounter(Long postId) {
        String key = RedisKeys.postCounter(postId);
        return redisCacheService.get(key)
                .flatMap(value -> {
                    try {
                        return Optional.of(objectMapper.readValue(value, CommunityPostCounters.class));
                    } catch (JsonProcessingException exception) {
                        log.warn("Redis counter deserialization failed for key {}: {}", key, exception.getMessage());
                        redisCacheService.delete(key);
                        return Optional.empty();
                    }
                });
    }

    private void writeCounter(Long postId, CommunityPostCounters counters) {
        try {
            redisCacheService.set(
                    RedisKeys.postCounter(postId),
                    objectMapper.writeValueAsString(counters),
                    COUNTER_TTL
            );
        } catch (JsonProcessingException exception) {
            log.warn("Redis counter serialization failed for post {}: {}", postId, exception.getMessage());
        }
    }

    private CommunityPostResponse applyCounters(CommunityPostResponse post, CommunityPostCounters counters) {
        if (counters == null) {
            return post;
        }
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
                counters.reactionCount(),
                counters.pigCount(),
                counters.favoriteCount(),
                counters.viewCount(),
                post.likedByCurrentUser(),
                post.piggedByCurrentUser(),
                post.favoritedByCurrentUser(),
                post.media(),
                post.collection(),
                post.lastActivityAt(),
                post.createdAt(),
                post.updatedAt()
        );
    }
}
