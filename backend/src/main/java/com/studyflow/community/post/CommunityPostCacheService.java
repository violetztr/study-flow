package com.studyflow.community.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.infrastructure.redis.RedisCacheService;
import com.studyflow.infrastructure.redis.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CommunityPostCacheService {
    private static final Logger log = LoggerFactory.getLogger(CommunityPostCacheService.class);
    private static final Duration SHORT_CACHE_TTL = Duration.ofMinutes(2);
    private static final String FEED_CONTENT_TYPE_ALL = "all";
    private static final int FIRST_PAGE = 0;
    private static final TypeReference<List<CommunityPostResponse>> FEED_TYPE = new TypeReference<>() {
    };

    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    public CommunityPostCacheService(RedisCacheService redisCacheService, ObjectMapper objectMapper) {
        this.redisCacheService = redisCacheService;
        this.objectMapper = objectMapper;
    }

    public Optional<List<CommunityPostResponse>> getFeed() {
        return redisCacheService.get(feedKey())
                .flatMap(value -> read(value, FEED_TYPE, feedKey()));
    }

    public void cacheFeed(List<CommunityPostResponse> posts) {
        write(feedKey(), posts);
    }

    public Optional<CommunityPostResponse> getPostDetail(Long postId) {
        String key = RedisKeys.postDetail(postId);
        return redisCacheService.get(key)
                .flatMap(value -> read(value, CommunityPostResponse.class, key));
    }

    public void cachePostDetail(Long postId, CommunityPostResponse post) {
        write(RedisKeys.postDetail(postId), post);
    }

    public void evictFeed() {
        redisCacheService.delete(feedKey());
    }

    public void evictPost(Long postId) {
        redisCacheService.delete(RedisKeys.postDetail(postId));
    }

    public void evictFeedAndPost(Long postId) {
        evictFeed();
        evictPost(postId);
    }

    private String feedKey() {
        return RedisKeys.feed(FEED_CONTENT_TYPE_ALL, FIRST_PAGE);
    }

    private void write(String key, Object value) {
        try {
            redisCacheService.set(key, objectMapper.writeValueAsString(value), SHORT_CACHE_TTL);
        } catch (JsonProcessingException exception) {
            log.warn("Redis cache serialization failed for key {}: {}", key, exception.getMessage());
        }
    }

    private <T> Optional<T> read(String value, Class<T> type, String key) {
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException exception) {
            log.warn("Redis cache deserialization failed for key {}: {}", key, exception.getMessage());
            redisCacheService.delete(key);
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(String value, TypeReference<T> type, String key) {
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException exception) {
            log.warn("Redis cache deserialization failed for key {}: {}", key, exception.getMessage());
            redisCacheService.delete(key);
            return Optional.empty();
        }
    }
}
