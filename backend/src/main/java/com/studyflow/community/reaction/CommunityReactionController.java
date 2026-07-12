package com.studyflow.community.reaction;

import com.studyflow.common.ApiResponse;
import com.studyflow.security.UserPrincipal;
import com.studyflow.wallet.dto.UserWalletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
public class CommunityReactionController {
    private final CommunityReactionService communityReactionService;

    public CommunityReactionController(CommunityReactionService communityReactionService) {
        this.communityReactionService = communityReactionService;
    }

    @PostMapping("/posts/{postId}/reactions/like")
    public ApiResponse<Void> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        communityReactionService.likePost(principal.userId(), postId);
        return ApiResponse.success();
    }

    @PostMapping("/posts/{postId}/reactions/pig")
    public ApiResponse<UserWalletResponse> pigPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(communityReactionService.pigPost(principal.userId(), postId));
    }

    @DeleteMapping("/posts/{postId}/reactions/like")
    public ApiResponse<Void> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        communityReactionService.unlikePost(principal.userId(), postId);
        return ApiResponse.success();
    }
}
