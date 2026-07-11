package com.studyflow.community.member;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.member.dto.CommunityMemberProfileResponse;
import com.studyflow.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/profiles")
public class CommunityMemberProfileController {
    private final CommunityMemberProfileService communityMemberProfileService;

    public CommunityMemberProfileController(CommunityMemberProfileService communityMemberProfileService) {
        this.communityMemberProfileService = communityMemberProfileService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<CommunityMemberProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId
    ) {
        Long currentUserId = principal == null ? null : principal.userId();
        return ApiResponse.success(communityMemberProfileService.getProfile(currentUserId, userId));
    }
}
