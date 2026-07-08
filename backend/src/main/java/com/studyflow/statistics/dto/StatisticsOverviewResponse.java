package com.studyflow.statistics.dto;

public record StatisticsOverviewResponse(
        long totalTasks,
        long completedTasks,
        long inProgressTasks,
        long overdueTasks,
        long totalEstimatedMinutes,
        long completedEstimatedMinutes
) {
}
