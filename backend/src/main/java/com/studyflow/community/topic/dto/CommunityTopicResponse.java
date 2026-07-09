package com.studyflow.community.topic.dto;

public record CommunityTopicResponse(
        Long id,
        String name,
        String slug,
        String description,
        String color,
        Integer postCount
) {
}
