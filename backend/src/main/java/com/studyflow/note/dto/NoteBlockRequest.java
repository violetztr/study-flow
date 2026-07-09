package com.studyflow.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteBlockRequest(
        @NotBlank(message = "块类型不能为空")
        @Size(max = 30, message = "块类型不能超过 30 个字符")
        String type,

        String content,

        Boolean checked,

        Integer sortOrder
) {
}
