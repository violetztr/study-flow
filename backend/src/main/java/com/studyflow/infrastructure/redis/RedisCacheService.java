package com.studyflow.infrastructure.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> get(String key) {
        return run("get", key, () -> Optional.ofNullable(redisTemplate.opsForValue().get(key)))
                .orElse(Optional.empty());
    }

    public void set(String key, String value, Duration ttl) {
        run("set", key, () -> {
            redisTemplate.opsForValue().set(key, value, ttl);
            return null;
        });
    }

    public Optional<Boolean> setIfAbsent(String key, String value, Duration ttl) {
        return run("setIfAbsent", key, () -> Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, value, ttl)))
                .orElse(Optional.empty());
    }

    public Optional<Long> increment(String key, Duration ttlWhenCreated) {
        return run("increment", key, () -> {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L && ttlWhenCreated != null && !ttlWhenCreated.isNegative()) {
                redisTemplate.expire(key, ttlWhenCreated);
            }
            return Optional.ofNullable(value);
        }).orElse(Optional.empty());
    }

    public void delete(String key) {
        run("delete", key, () -> {
            redisTemplate.delete(key);
            return null;
        });
    }

    // ---- ZSet operations ----

    public Optional<Boolean> zadd(String key, String member, double score) {
        return run("zadd", key, () -> redisTemplate.opsForZSet().add(key, member, score));
    }

    public Optional<Set<String>> zrevrange(String key, long start, long end) {
        return run("zrevrange", key, () -> redisTemplate.opsForZSet().reverseRange(key, start, end));
    }

    public void zremrangeByRank(String key, long start, long end) {
        run("zremrangeByRank", key, () -> {
            redisTemplate.opsForZSet().removeRange(key, start, end);
            return null;
        });
    }

    public void zrem(String key, String member) {
        run("zrem", key, () -> {
            redisTemplate.opsForZSet().remove(key, member);
            return null;
        });
    }

    private <T> Optional<T> run(String operation, String key, Supplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn("Redis {} failed for key {}: {}", operation, key, ex.getMessage());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Redis {} failed open for key {}: {}", operation, key, ex.getMessage());
            return Optional.empty();
        }
    }
}
