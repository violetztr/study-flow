package com.studyflow.community.comment;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.comment.dto.CommunityCommentRequest;
import com.studyflow.community.comment.dto.CommunityCommentResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityCommentController {
    private final CommunityCommentService communityCommentService;

    public CommunityCommentController(CommunityCommentService communityCommentService) {
        this.communityCommentService = communityCommentService;
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommunityCommentResponse>> listComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(communityCommentService.listComments(currentUserId(principal), postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentResponse> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityCommentRequest request
    ) {
        return ApiResponse.success(communityCommentService.createComment(principal.userId(), postId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId
    ) {
        communityCommentService.deleteComment(principal.userId(), commentId);
        return ApiResponse.success();
    }

    private Long currentUserId(UserPrincipal principal) {
        return principal == null ? null : principal.userId();
    }
}
