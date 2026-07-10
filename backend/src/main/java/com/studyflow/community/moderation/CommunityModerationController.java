package com.studyflow.community.moderation;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.moderation.dto.ModerationRequest;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/community")
public class CommunityModerationController {
    private final CommunityModerationService communityModerationService;

    public CommunityModerationController(CommunityModerationService communityModerationService) {
        this.communityModerationService = communityModerationService;
    }

    @PostMapping("/posts/{postId}/hide")
    public ApiResponse<Void> hidePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody(required = false) ModerationRequest request
    ) {
        communityModerationService.hidePost(principal.userId(), postId, request);
        return ApiResponse.success();
    }

    @PostMapping("/posts/{postId}/restore")
    public ApiResponse<Void> restorePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody(required = false) ModerationRequest request
    ) {
        communityModerationService.restorePost(principal.userId(), postId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        communityModerationService.deletePost(principal.userId(), postId);
        return ApiResponse.success();
    }

    @PostMapping("/comments/{commentId}/hide")
    public ApiResponse<Void> hideComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody(required = false) ModerationRequest request
    ) {
        communityModerationService.hideComment(principal.userId(), commentId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId
    ) {
        communityModerationService.deleteComment(principal.userId(), commentId);
        return ApiResponse.success();
    }

    @DeleteMapping("/danmaku/{danmakuId}")
    public ApiResponse<Void> deleteDanmaku(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long danmakuId
    ) {
        communityModerationService.deleteDanmaku(principal.userId(), danmakuId);
        return ApiResponse.success();
    }

    @PostMapping("/comments/{commentId}/restore")
    public ApiResponse<Void> restoreComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody(required = false) ModerationRequest request
    ) {
        communityModerationService.restoreComment(principal.userId(), commentId, request);
        return ApiResponse.success();
    }

    @PostMapping("/members/{userId}/mute")
    public ApiResponse<Void> muteMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody(required = false) ModerationRequest request
    ) {
        communityModerationService.muteMember(principal.userId(), userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/members/{userId}/unmute")
    public ApiResponse<Void> unmuteMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody(required = false) ModerationRequest request
    ) {
        communityModerationService.unmuteMember(principal.userId(), userId, request);
        return ApiResponse.success();
    }
}
