package com.studyflow.daily.dto;

import com.studyflow.daily.DailyPlan;

import java.time.LocalDate;

public record DailyPlanResponse(
        Long id,
        LocalDate planDate,
        String title,
        String description,
        String status
) {
    public static DailyPlanResponse from(DailyPlan plan) {
        return new DailyPlanResponse(
                plan.getId(),
                plan.getPlanDate(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getStatus()
        );
    }
}
