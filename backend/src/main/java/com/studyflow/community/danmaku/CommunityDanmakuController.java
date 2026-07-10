package com.studyflow.community.danmaku;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.danmaku.dto.CommunityDanmakuRequest;
import com.studyflow.community.danmaku.dto.CommunityDanmakuResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityDanmakuController {
    private final CommunityDanmakuService communityDanmakuService;

    public CommunityDanmakuController(CommunityDanmakuService communityDanmakuService) {
        this.communityDanmakuService = communityDanmakuService;
    }

    @GetMapping("/posts/{postId}/danmaku")
    public ApiResponse<List<CommunityDanmakuResponse>> listDanmaku(@PathVariable Long postId) {
        return ApiResponse.success(communityDanmakuService.listDanmaku(postId));
    }

    @PostMapping("/posts/{postId}/danmaku")
    public ApiResponse<CommunityDanmakuResponse> createDanmaku(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityDanmakuRequest request
    ) {
        return ApiResponse.success(communityDanmakuService.createDanmaku(principal.userId(), postId, request));
    }
}
