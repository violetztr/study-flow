package com.studyflow.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitRuleTest {
    @Test
    void matchesSensitiveWriteEndpointsWithExpectedActions() {
        assertThat(RateLimitRule.match("POST", "/api/auth/login"))
                .map(RateLimitRule::action)
                .contains("login");
        assertThat(RateLimitRule.match("POST", "/api/auth/register"))
                .map(RateLimitRule::action)
                .contains("register");
        assertThat(RateLimitRule.match("POST", "/api/media/uploads/presign"))
                .map(RateLimitRule::action)
                .contains("upload");
        assertThat(RateLimitRule.match("POST", "/api/community/posts/12/comments"))
                .map(RateLimitRule::action)
                .contains("comment");
        assertThat(RateLimitRule.match("POST", "/api/community/posts/12/danmaku"))
                .map(RateLimitRule::action)
                .contains("danmaku");
        assertThat(RateLimitRule.match("POST", "/api/community/posts/12/reactions/like"))
                .map(RateLimitRule::action)
                .contains("like");
        assertThat(RateLimitRule.match("POST", "/api/community/posts/12/reactions/pig"))
                .map(RateLimitRule::action)
                .contains("pig");
        assertThat(RateLimitRule.match("POST", "/api/community/posts/12/favorites"))
                .map(RateLimitRule::action)
                .contains("favorite");
        assertThat(RateLimitRule.match("POST", "/api/community/posts/12/views"))
                .map(RateLimitRule::action)
                .contains("view");
    }

    @Test
    void ignoresReadEndpointsAndUnrelatedWrites() {
        assertThat(RateLimitRule.match("GET", "/api/community/feed")).isEmpty();
        assertThat(RateLimitRule.match("GET", "/api/community/posts/12/comments")).isEmpty();
        assertThat(RateLimitRule.match("POST", "/api/community/posts")).isEmpty();
    }
}
