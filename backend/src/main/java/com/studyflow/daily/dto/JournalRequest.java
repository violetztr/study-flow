package com.studyflow.daily.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record JournalRequest(
        @NotNull(message = "日记日期不能为空")
        LocalDate journalDate,

        @Size(max = 30, message = "心情不能超过 30 个字符")
        String mood,

        String content
) {
}
