package com.studyflow.note;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.note.dto.NoteBlockRequest;
import com.studyflow.note.dto.NoteBlockResponse;
import com.studyflow.note.dto.NoteRequest;
import com.studyflow.note.dto.NoteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {
    private final NoteMapper noteMapper;
    private final NoteBlockMapper noteBlockMapper;

    public NoteService(NoteMapper noteMapper, NoteBlockMapper noteBlockMapper) {
        this.noteMapper = noteMapper;
        this.noteBlockMapper = noteBlockMapper;
    }

    @Transactional
    public NoteResponse createNote(Long userId, NoteRequest request) {
        if (request.parentId() != null) {
            requireOwnedNote(userId, request.parentId());
        }

        Note note = new Note();
        note.setUserId(userId);
        note.setParentId(request.parentId());
        note.setTitle(request.title());
        note.setIcon(request.icon());
        note.setFavorite(Boolean.TRUE.equals(request.favorite()));
        note.setArchived(false);
        note.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        noteMapper.insert(note);
        return NoteResponse.from(note);
    }

    public List<NoteResponse> listNotes(Long userId) {
        return noteMapper.selectList(new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .eq(Note::getArchived, false)
                        .orderByAsc(Note::getSortOrder)
                        .orderByDesc(Note::getId))
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    public NoteResponse getNote(Long userId, Long noteId) {
        Note note = requireOwnedNote(userId, noteId);
        return NoteResponse.from(note, listBlockResponses(note.getId()));
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteRequest request) {
        Note note = requireOwnedNote(userId, noteId);
        if (request.parentId() != null && !request.parentId().equals(noteId)) {
            requireOwnedNote(userId, request.parentId());
        }
        if (request.parentId() != null && request.parentId().equals(noteId)) {
            throw new BusinessException(400, "笔记不能作为自己的父级");
        }

        note.setParentId(request.parentId());
        note.setTitle(request.title());
        note.setIcon(request.icon());
        note.setFavorite(Boolean.TRUE.equals(request.favorite()));
        note.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        noteMapper.updateById(note);
        return NoteResponse.from(note, listBlockResponses(note.getId()));
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        Note note = requireOwnedNote(userId, noteId);
        note.setArchived(true);
        noteMapper.updateById(note);
    }

    @Transactional
    public NoteResponse replaceBlocks(Long userId, Long noteId, List<NoteBlockRequest> requests) {
        Note note = requireOwnedNote(userId, noteId);
        noteBlockMapper.delete(new LambdaQueryWrapper<NoteBlock>().eq(NoteBlock::getNoteId, noteId));

        for (int i = 0; i < requests.size(); i++) {
            NoteBlockRequest request = requests.get(i);
            NoteBlock block = new NoteBlock();
            block.setNoteId(noteId);
            block.setUserId(userId);
            block.setType(normalizeBlockType(request.type()));
            block.setContent(request.content());
            block.setChecked(Boolean.TRUE.equals(request.checked()));
            block.setSortOrder(request.sortOrder() == null ? i : request.sortOrder());
            noteBlockMapper.insert(block);
        }

        return NoteResponse.from(note, listBlockResponses(noteId));
    }

    private Note requireOwnedNote(Long userId, Long noteId) {
        Note note = noteMapper.selectOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getArchived, false));
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        return note;
    }

    private List<NoteBlockResponse> listBlockResponses(Long noteId) {
        return noteBlockMapper.selectList(new LambdaQueryWrapper<NoteBlock>()
                        .eq(NoteBlock::getNoteId, noteId)
                        .orderByAsc(NoteBlock::getSortOrder)
                        .orderByAsc(NoteBlock::getId))
                .stream()
                .map(NoteBlockResponse::from)
                .toList();
    }

    private String normalizeBlockType(String type) {
        if (type.equals("paragraph")
                || type.equals("heading")
                || type.equals("todo")
                || type.equals("quote")
                || type.equals("code")) {
            return type;
        }
        throw new BusinessException(400, "笔记块类型不正确");
    }
}
