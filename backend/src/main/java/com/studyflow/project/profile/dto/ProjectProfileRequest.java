package com.studyflow.project.profile.dto;

import jakarta.validation.constraints.Size;

public record ProjectProfileRequest(
        @Size(max = 160, message = "项目一句话介绍不能超过 160 个字符")
        String headline,

        @Size(max = 300, message = "线上地址不能超过 300 个字符")
        String productionUrl,

        @Size(max = 300, message = "接口文档地址不能超过 300 个字符")
        String apiDocUrl,

        @Size(max = 300, message = "数据库文档地址不能超过 300 个字符")
        String databaseDocUrl,

        String architectureSummary,

        String interviewHighlights,

        @Size(max = 500, message = "封面图地址不能超过 500 个字符")
        String coverImageUrl
) {
}
