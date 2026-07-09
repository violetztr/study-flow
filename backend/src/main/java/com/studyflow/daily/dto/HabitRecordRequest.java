package com.studyflow.daily.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HabitRecordRequest(
        @NotNull(message = "打卡日期不能为空")
        LocalDate recordDate,

        Boolean completed
) {
}
