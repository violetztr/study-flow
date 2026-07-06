package com.studyflow.tag;

import com.studyflow.common.ApiResponse;
import com.studyflow.security.UserPrincipal;
import com.studyflow.tag.dto.TagRequest;
import com.studyflow.tag.dto.TagResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> listTags(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.listTags(principal.userId()));
    }

    @PostMapping
    public ApiResponse<TagResponse> createTag(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TagRequest request
    ) {
        return ApiResponse.success(tagService.createTag(principal.userId(), request));
    }
}
