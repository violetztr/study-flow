package com.studyflow.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheServiceTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisCacheService redisCacheService = new RedisCacheService(redisTemplate);

    @Test
    void readsAndWritesStringValuesWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ruru:test")).thenReturn("cached");

        assertThat(redisCacheService.get("ruru:test")).contains("cached");

        redisCacheService.set("ruru:test", "next", Duration.ofMinutes(5));

        verify(valueOperations).set("ruru:test", "next", Duration.ofMinutes(5));
    }

    @Test
    void incrementsAndAddsTtlWhenKeyIsCreated() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("ruru:counter:post:1")).thenReturn(1L);

        Optional<Long> value = redisCacheService.increment("ruru:counter:post:1", Duration.ofHours(1));

        assertThat(value).contains(1L);
        verify(redisTemplate).expire("ruru:counter:post:1", Duration.ofHours(1));
    }

    @Test
    void supportsSetIfAbsentForDedupeKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("ruru:view:dedupe:1:user_7", "1", Duration.ofHours(6))).thenReturn(true);

        Optional<Boolean> stored = redisCacheService.setIfAbsent(
                "ruru:view:dedupe:1:user_7",
                "1",
                Duration.ofHours(6)
        );

        assertThat(stored).contains(true);
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ruru:test")).thenThrow(new QueryTimeoutException("redis timeout"));
        when(valueOperations.increment("ruru:test")).thenThrow(new QueryTimeoutException("redis timeout"));

        assertThat(redisCacheService.get("ruru:test")).isEmpty();
        assertThat(redisCacheService.increment("ruru:test", Duration.ofMinutes(1))).isEmpty();
        assertThatCode(() -> redisCacheService.set("ruru:test", "value", Duration.ofMinutes(1)))
                .doesNotThrowAnyException();
        assertThatCode(() -> redisCacheService.delete("ruru:test"))
                .doesNotThrowAnyException();
    }
}
