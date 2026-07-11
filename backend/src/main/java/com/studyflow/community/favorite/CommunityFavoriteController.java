package com.studyflow.community.favorite;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.post.CommunityPostService;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityFavoriteController {
    private final CommunityFavoriteService communityFavoriteService;
    private final CommunityPostService communityPostService;

    public CommunityFavoriteController(
            CommunityFavoriteService communityFavoriteService,
            CommunityPostService communityPostService
    ) {
        this.communityFavoriteService = communityFavoriteService;
        this.communityPostService = communityPostService;
    }

    @PostMapping("/posts/{postId}/favorites")
    public ApiResponse<Void> favoritePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        communityFavoriteService.favoritePost(principal.userId(), postId);
        return ApiResponse.success();
    }

    @DeleteMapping("/posts/{postId}/favorites")
    public ApiResponse<Void> unfavoritePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        communityFavoriteService.unfavoritePost(principal.userId(), postId);
        return ApiResponse.success();
    }

    @GetMapping("/favorites/my")
    public ApiResponse<List<CommunityPostResponse>> listMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(communityPostService.listFavoritePosts(principal.userId()));
    }
}
