package com.studyflow.community.post.dto;

import java.util.List;

public record CommunityCollectionItemsPageResponse(
        Long collectionId,
        Integer page,
        Integer pageSize,
        Long total,
        Boolean hasNext,
        List<CommunityCollectionItemResponse> items
) {
}
