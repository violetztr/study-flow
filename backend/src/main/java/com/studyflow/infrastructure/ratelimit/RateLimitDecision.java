package com.studyflow.infrastructure.ratelimit;

public record RateLimitDecision(boolean allowed, long current, int limit, String message) {
    public static RateLimitDecision allowed(long current, int limit) {
        return new RateLimitDecision(true, current, limit, "success");
    }

    public static RateLimitDecision limited(long current, int limit) {
        return new RateLimitDecision(false, current, limit, "请求太频繁，请稍后再试");
    }
}
