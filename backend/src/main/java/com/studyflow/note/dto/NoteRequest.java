package com.studyflow.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        Long parentId,

        @NotBlank(message = "笔记标题不能为空")
        @Size(max = 120, message = "笔记标题不能超过 120 个字符")
        String title,

        @Size(max = 50, message = "图标不能超过 50 个字符")
        String icon,

        Boolean favorite,

        Integer sortOrder
) {
}
