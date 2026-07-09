package com.studyflow.daily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DailyPlanRequest(
        @NotNull(message = "计划日期不能为空")
        LocalDate planDate,

        @NotBlank(message = "计划标题不能为空")
        @Size(max = 120, message = "计划标题不能超过 120 个字符")
        String title,

        @Size(max = 500, message = "计划描述不能超过 500 个字符")
        String description,

        String status
) {
}
