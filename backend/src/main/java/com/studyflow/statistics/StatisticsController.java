package com.studyflow.statistics;

import com.studyflow.common.ApiResponse;
import com.studyflow.security.UserPrincipal;
import com.studyflow.statistics.dto.StatisticsOverviewResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> overview(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(statisticsService.overview(principal.userId()));
    }
}
