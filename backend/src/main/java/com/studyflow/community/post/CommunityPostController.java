package com.studyflow.community.post;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.post.dto.CommunityPostRequest;
import com.studyflow.community.post.dto.CommunityPostResponse;
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
@RequestMapping("/api/community")
public class CommunityPostController {
    private final CommunityPostService communityPostService;

    public CommunityPostController(CommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @GetMapping("/feed")
    public ApiResponse<List<CommunityPostResponse>> listFeed(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(communityPostService.listFeed(currentUserId(principal)));
    }

    @GetMapping("/submissions/my")
    public ApiResponse<List<CommunityPostResponse>> listMySubmissions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(communityPostService.listMySubmissions(principal.userId()));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityPostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        return ApiResponse.success(communityPostService.createPost(principal.userId(), request));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(communityPostService.getPost(currentUserId(principal), postId));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        return ApiResponse.success(communityPostService.updatePost(principal.userId(), postId, request));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        communityPostService.deletePost(principal.userId(), postId);
        return ApiResponse.success();
    }

    private Long currentUserId(UserPrincipal principal) {
        return principal == null ? null : principal.userId();
    }
}
