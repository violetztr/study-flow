package com.studyflow.note.dto;

import com.studyflow.note.Note;

import java.util.List;

public record NoteResponse(
        Long id,
        Long parentId,
        String title,
        String icon,
        Boolean favorite,
        Boolean archived,
        Integer sortOrder,
        List<NoteBlockResponse> blocks
) {
    public static NoteResponse from(Note note) {
        return from(note, List.of());
    }

    public static NoteResponse from(Note note, List<NoteBlockResponse> blocks) {
        return new NoteResponse(
                note.getId(),
                note.getParentId(),
                note.getTitle(),
                note.getIcon(),
                Boolean.TRUE.equals(note.getFavorite()),
                Boolean.TRUE.equals(note.getArchived()),
                note.getSortOrder(),
                blocks
        );
    }
}
