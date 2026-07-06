package com.studyflow.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称不能超过 50 个字符")
        String name,

        @Size(max = 20, message = "标签颜色不能超过 20 个字符")
        String color
) {
}
