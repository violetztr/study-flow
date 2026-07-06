package com.studyflow.tag.dto;

import com.studyflow.tag.Tag;

public record TagResponse(Long id, String name, String color) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }
}
