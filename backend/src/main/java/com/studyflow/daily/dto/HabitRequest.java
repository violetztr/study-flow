package com.studyflow.daily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabitRequest(
        @NotBlank(message = "习惯名称不能为空")
        @Size(max = 80, message = "习惯名称不能超过 80 个字符")
        String name,

        @Size(max = 300, message = "习惯描述不能超过 300 个字符")
        String description
) {
}
