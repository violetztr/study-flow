package com.studyflow.infrastructure.ratelimit;

import com.studyflow.infrastructure.redis.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "study-flow.rate-limit.enabled=true")
class RateLimitIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisCacheService redisCacheService;

    @Test
    void blocksLoginRequestWhenRedisCounterExceedsLimit() throws Exception {
        when(redisCacheService.increment(eq("ruru:rate:login:127.0.0.1"), eq(Duration.ofMinutes(1))))
                .thenReturn(Optional.of(9L));

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("请求太频繁，请稍后再试"));
    }

    @Test
    void failsOpenWhenRedisCounterIsUnavailable() throws Exception {
        when(redisCacheService.increment(eq("ruru:rate:login:127.0.0.1"), eq(Duration.ofMinutes(1))))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
