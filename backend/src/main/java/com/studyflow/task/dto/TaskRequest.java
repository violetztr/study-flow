package com.studyflow.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record TaskRequest(
        @NotNull(message = "项目 ID 不能为空")
        Long projectId,

        @NotBlank(message = "任务标题不能为空")
        @Size(max = 120, message = "任务标题不能超过 120 个字符")
        String title,

        String description,
        String status,
        String priority,
        LocalDateTime deadline,
        @PositiveOrZero(message = "预计学习时长不能小于 0")
        Integer estimatedMinutes,
        List<Long> tagIds
) {
}
