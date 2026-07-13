package com.studyflow.community.post.dto;

import jakarta.validation.constraints.Size;

public record CommunityPostCollectionRequest(
        Boolean enabled,
        Long collectionId,
        @Size(max = 160) String title,
        @Size(max = 1000) String description
) {
}
