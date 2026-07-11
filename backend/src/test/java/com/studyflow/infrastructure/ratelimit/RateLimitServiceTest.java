package com.studyflow.infrastructure.ratelimit;

import com.studyflow.infrastructure.redis.RedisCacheService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {
    private final RedisCacheService redisCacheService = mock(RedisCacheService.class);
    private final RateLimitService rateLimitService = new RateLimitService(redisCacheService);

    @Test
    void allowsRequestsWhenCounterIsWithinLimit() {
        RateLimitRule rule = new RateLimitRule("comment", 5, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP);
        when(redisCacheService.increment("ruru:rate:comment:user_7", Duration.ofMinutes(1))).thenReturn(Optional.of(5L));

        RateLimitDecision decision = rateLimitService.check(rule, "user:7");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.current()).isEqualTo(5L);
        assertThat(decision.limit()).isEqualTo(5);
    }

    @Test
    void blocksRequestsWhenCounterExceedsLimit() {
        RateLimitRule rule = new RateLimitRule("login", 5, Duration.ofMinutes(1), RateLimitIdentity.IP);
        when(redisCacheService.increment("ruru:rate:login:127.0.0.1", Duration.ofMinutes(1))).thenReturn(Optional.of(6L));

        RateLimitDecision decision = rateLimitService.check(rule, "127.0.0.1");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.message()).isEqualTo("请求太频繁，请稍后再试");
    }

    @Test
    void failsOpenWhenRedisCounterIsUnavailable() {
        RateLimitRule rule = new RateLimitRule("upload", 10, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP);
        when(redisCacheService.increment("ruru:rate:upload:user_7", Duration.ofMinutes(1))).thenReturn(Optional.empty());

        RateLimitDecision decision = rateLimitService.check(rule, "user:7");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.current()).isZero();
    }
}
