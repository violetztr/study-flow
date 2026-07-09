package com.studyflow.note;

import com.studyflow.common.ApiResponse;
import com.studyflow.note.dto.NoteBlockRequest;
import com.studyflow.note.dto.NoteRequest;
import com.studyflow.note.dto.NoteResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ApiResponse<List<NoteResponse>> listNotes(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(noteService.listNotes(principal.userId()));
    }

    @PostMapping
    public ApiResponse<NoteResponse> createNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NoteRequest request
    ) {
        return ApiResponse.success(noteService.createNote(principal.userId(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteResponse> getNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(noteService.getNote(principal.userId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteResponse> updateNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request
    ) {
        return ApiResponse.success(noteService.updateNote(principal.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        noteService.deleteNote(principal.userId(), id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/blocks")
    public ApiResponse<NoteResponse> replaceBlocks(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody List<NoteBlockRequest> requests
    ) {
        return ApiResponse.success(noteService.replaceBlocks(principal.userId(), id, requests));
    }
}
