package com.studyflow.community.member;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.member.dto.CommunityMemberResponse;
import com.studyflow.community.member.dto.UserProfileRequest;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community/members")
public class CommunityMemberController {
    private final CommunityMemberService communityMemberService;

    public CommunityMemberController(CommunityMemberService communityMemberService) {
        this.communityMemberService = communityMemberService;
    }

    @GetMapping("/me")
    public ApiResponse<CommunityMemberResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(communityMemberService.getCurrentMember(principal.userId()));
    }

    @GetMapping
    public ApiResponse<List<CommunityMemberResponse>> listMembers(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(communityMemberService.listMembers(principal.userId()));
    }

    @GetMapping("/{userId}")
    public ApiResponse<CommunityMemberResponse> getMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId
    ) {
        return ApiResponse.success(communityMemberService.getMember(principal.userId(), userId));
    }

    @PutMapping("/me/profile")
    public ApiResponse<CommunityMemberResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserProfileRequest request
    ) {
        return ApiResponse.success(communityMemberService.updateCurrentProfile(principal.userId(), request));
    }
}
