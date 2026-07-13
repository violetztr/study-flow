package com.studyflow.community.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CommunityPostRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 10000) String content,
        Long topicId,
        @Size(max = 10) String topicName,
        Long videoCoverMediaFileId,
        @Size(max = 9) List<@NotNull Long> mediaFileIds,
        Boolean collectionEnabled,
        Long collectionId,
        @Size(max = 160) String collectionTitle,
        @Size(max = 1000) String collectionDescription
) {
}
