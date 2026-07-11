package com.studyflow.infrastructure.redis;

import java.util.Locale;

public final class RedisKeys {
    private static final String PREFIX = "ruru";
    private static final String UNKNOWN = "unknown";

    private RedisKeys() {
    }

    public static String rate(String action, String identity) {
        return join("rate", action, identity);
    }

    public static String viewDedupe(Long postId, String viewerKey) {
        return join("view", "dedupe", String.valueOf(postId), viewerKey);
    }

    public static String feed(String contentType, int page) {
        return join("feed", contentType, String.valueOf(page));
    }

    public static String postDetail(Long postId) {
        return join("post", "detail", String.valueOf(postId));
    }

    public static String postCounter(Long postId) {
        return join("counter", "post", String.valueOf(postId));
    }

    private static String join(String... segments) {
        StringBuilder key = new StringBuilder(PREFIX);
        for (String segment : segments) {
            key.append(':').append(normalize(segment));
        }
        return key.toString();
    }

    private static String normalize(String segment) {
        if (segment == null) {
            return UNKNOWN;
        }
        String normalized = segment.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? UNKNOWN : normalized;
    }
}
