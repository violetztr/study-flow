package com.studyflow.note.dto;

import com.studyflow.note.NoteBlock;

public record NoteBlockResponse(
        Long id,
        String type,
        String content,
        Boolean checked,
        Integer sortOrder
) {
    public static NoteBlockResponse from(NoteBlock block) {
        return new NoteBlockResponse(
                block.getId(),
                block.getType(),
                block.getContent(),
                Boolean.TRUE.equals(block.getChecked()),
                block.getSortOrder()
        );
    }
}
