package com.studyflow.community.member.dto;

import com.studyflow.community.post.dto.CommunityPostResponse;

import java.util.List;

public record CommunityMemberProfileResponse(
        CommunityMemberResponse member,
        List<CommunityPostResponse> posts,
        List<CommunityPostResponse> favoritePosts,
        Integer totalPostCount,
        Integer articleCount,
        Integer videoCount,
        Integer liveCount,
        Boolean currentUserProfile
) {
}
