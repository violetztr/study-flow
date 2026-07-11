package com.studyflow.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimitService, new ObjectMapper());

    @Test
    void returnsTooManyRequestsWithApiResponseBodyWhenLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.check(any(RateLimitRule.class), eq("127.0.0.1")))
                .thenReturn(new RateLimitDecision(false, 6, 5, "请求太频繁，请稍后再试"));

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("\"code\":429");
        assertThat(response.getContentAsString()).contains("请求太频繁，请稍后再试");
    }

    @Test
    void usesAuthenticatedUserIdentityWhenRuleAllowsUserIdentity() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "alice");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/community/posts/12/comments");
            request.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitService.check(
                    new RateLimitRule("comment", 12, Duration.ofMinutes(1), RateLimitIdentity.USER_OR_IP),
                    "user:7"
            )).thenReturn(RateLimitDecision.allowed(1, 12));

            boolean proceed = interceptor.preHandle(request, response, new Object());

            assertThat(proceed).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
