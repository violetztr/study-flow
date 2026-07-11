package com.studyflow.infrastructure.ratelimit;

import com.studyflow.infrastructure.redis.RedisCacheService;
import com.studyflow.infrastructure.redis.RedisKeys;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private final RedisCacheService redisCacheService;

    public RateLimitService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public RateLimitDecision check(RateLimitRule rule, String identity) {
        String key = RedisKeys.rate(rule.action(), identity);
        return redisCacheService.increment(key, rule.window())
                .map(current -> current > rule.limit()
                        ? RateLimitDecision.limited(current, rule.limit())
                        : RateLimitDecision.allowed(current, rule.limit()))
                .orElseGet(() -> RateLimitDecision.allowed(0, rule.limit()));
    }
}
