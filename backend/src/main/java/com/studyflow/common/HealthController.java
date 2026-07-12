package com.studyflow.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse(
                "UP",
                "ruru-community",
                LocalDateTime.now()
        ));
    }

    public record HealthResponse(
            String status,
            String service,
            LocalDateTime checkedAt
    ) {
    }
}
