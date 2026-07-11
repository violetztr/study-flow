package com.studyflow.infrastructure.ratelimit;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record RateLimitRule(String action, int limit, Duration window, RateLimitIdentity identity) {
    private static final List<RouteRule> RULES = List.of(
            new RouteRule("POST", Pattern.compile("^/api/auth/login$"),
                    new RateLimitRule("login", 8, Duration.ofMinutes(1), RateLimitIdentity.IP)),
            new RouteRule("POST", Pattern.compile("^/api/auth/register$"),
                    new RateLimitRule("register", 5, Duration.ofMinutes(5), RateLimitIdentity.IP)),
            new RouteRule("POST", Pattern.compile("^/api/media/uploads/presign$"),
                    new RateLimitRule("upload", 20, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP)),
            new RouteRule("POST", Pattern.compile("^/api/community/posts/\\d+/comments$"),
                    new RateLimitRule("comment", 12, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP)),
            new RouteRule("POST", Pattern.compile("^/api/community/posts/\\d+/danmaku$"),
                    new RateLimitRule("danmaku", 30, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP)),
            new RouteRule("POST", Pattern.compile("^/api/community/posts/\\d+/reactions/like$"),
                    new RateLimitRule("like", 60, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP)),
            new RouteRule("POST", Pattern.compile("^/api/community/posts/\\d+/reactions/pig$"),
                    new RateLimitRule("pig", 20, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP)),
            new RouteRule("POST", Pattern.compile("^/api/community/posts/\\d+/favorites$"),
                    new RateLimitRule("favorite", 60, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP)),
            new RouteRule("POST", Pattern.compile("^/api/community/posts/\\d+/views$"),
                    new RateLimitRule("view", 120, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP))
    );

    public static Optional<RateLimitRule> match(String method, String path) {
        return RULES.stream()
                .filter(rule -> rule.matches(method, path))
                .map(RouteRule::rateLimitRule)
                .findFirst();
    }

    private record RouteRule(String method, Pattern pathPattern, RateLimitRule rateLimitRule) {
        boolean matches(String requestMethod, String requestPath) {
            return method.equalsIgnoreCase(requestMethod) && pathPattern.matcher(requestPath).matches();
        }
    }
}
