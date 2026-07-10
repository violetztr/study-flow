package com.studyflow.community.danmaku.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunityDanmakuRequest(
        @NotBlank @Size(max = 200) String content,
        @NotNull @Min(0) Integer timeSeconds,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color
) {
}
