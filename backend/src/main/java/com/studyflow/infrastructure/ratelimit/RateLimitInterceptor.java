package com.studyflow.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.common.ApiResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "study-flow.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        return RateLimitRule.match(request.getMethod(), request.getRequestURI())
                .map(rule -> checkRule(rule, request, response))
                .orElse(true);
    }

    private boolean checkRule(RateLimitRule rule, HttpServletRequest request, HttpServletResponse response) {
        RateLimitDecision decision = rateLimitService.check(rule, identity(rule, request));
        if (decision.allowed()) {
            return true;
        }
        writeLimitedResponse(response, decision);
        return false;
    }

    private String identity(RateLimitRule rule, HttpServletRequest request) {
        if (RateLimitIdentity.USER_OR_IP.equals(rule.identity())) {
            Long userId = currentUserId();
            if (userId != null) {
                return "user:" + userId;
            }
        }
        return clientIp(request);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void writeLimitedResponse(HttpServletResponse response, RateLimitDecision decision) {
        try {
            response.setStatus(HTTP_TOO_MANY_REQUESTS);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error(HTTP_TOO_MANY_REQUESTS, decision.message())
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write rate limit response", exception);
        }
    }
}
