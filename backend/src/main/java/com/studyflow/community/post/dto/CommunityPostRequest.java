package com.studyflow.community.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 10000) String content,
        Long topicId
) {
}
