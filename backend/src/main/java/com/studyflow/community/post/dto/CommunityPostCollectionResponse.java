package com.studyflow.community.post.dto;

import java.util.List;

public record CommunityPostCollectionResponse(
        Long id,
        String title,
        String description,
        List<CommunityCollectionItemResponse> items
) {
}
