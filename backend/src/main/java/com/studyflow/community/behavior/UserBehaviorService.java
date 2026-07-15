package com.studyflow.community.behavior;

import com.studyflow.infrastructure.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class UserBehaviorService {
    private static final Logger log = LoggerFactory.getLogger(UserBehaviorService.class);
    private static final Duration DEDUPE_TTL = Duration.ofHours(24);
    private static final String TARGET_POST = "POST";
    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_LIKE = "LIKE";
    private static final String ACTION_FAVORITE = "FAVORITE";
    private static final String ACTION_FOLLOW = "FOLLOW";
    private static final String ACTION_SEARCH = "SEARCH";

    private final UserBehaviorMapper userBehaviorMapper;
    private final RedisCacheService redisCacheService;

    public UserBehaviorService(
            UserBehaviorMapper userBehaviorMapper,
            RedisCacheService redisCacheService
    ) {
        this.userBehaviorMapper = userBehaviorMapper;
        this.redisCacheService = redisCacheService;
    }

    public void recordPostView(Long userId, Long postId) {
        record(userId, TARGET_POST, postId, ACTION_VIEW, null);
    }

    public void recordPostLike(Long userId, Long postId) {
        record(userId, TARGET_POST, postId, ACTION_LIKE, null);
    }

    public void recordPostFavorite(Long userId, Long postId) {
        record(userId, TARGET_POST, postId, ACTION_FAVORITE, null);
    }

    public void recordFollow(Long userId, Long targetUserId) {
        record(userId, "USER", targetUserId, ACTION_FOLLOW, null);
    }

    public void recordSearch(Long userId, String keyword) {
        record(userId, "KEYWORD", null, ACTION_SEARCH, keyword);
    }

    private void record(Long userId, String targetType, Long targetId, String action, String extra) {
        String dedupeTargetId = targetId != null ? String.valueOf(targetId) : extra;
        if (dedupeTargetId == null) {
            return;
        }
        String dedupeKey = "ruru:behavior:dedupe:" + safe(userId) + ":" + dedupeTargetId + ":" + action;
        if (redisCacheService.setIfAbsent(dedupeKey, "1", DEDUPE_TTL).orElse(false)) {
            try {
                UserBehavior behavior = new UserBehavior();
                behavior.setUserId(userId);
                behavior.setTargetType(targetType);
                behavior.setTargetId(targetId);
                behavior.setAction(action);
                behavior.setExtra(extra);
                behavior.setCreatedAt(LocalDateTime.now());
                userBehaviorMapper.insert(behavior);
            } catch (Exception ex) {
                log.warn("User behavior record failed: {}", ex.getMessage());
            }
        }
    }

    private String safe(Object value) {
        return value == null ? "anon" : value.toString();
    }
}
