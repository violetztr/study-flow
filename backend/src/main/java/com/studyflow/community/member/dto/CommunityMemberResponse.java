package com.studyflow.community.member.dto;

import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.UserProfile;

public record CommunityMemberResponse(
        Long userId,
        String username,
        String role,
        String memberStatus,
        Long circleId,
        String circleName,
        String circleSlug,
        String displayName,
        String bio,
        String avatarUrl,
        String profileBackgroundUrl,
        String profileBackgroundType,
        String skills,
        String githubUrl,
        String websiteUrl,
        Integer followerCount,
        Integer followingCount,
        Boolean followedByCurrentUser
) {
    public static CommunityMemberResponse from(
            Circle circle,
            CircleMember member,
            UserProfile profile,
            String username
    ) {
        return new CommunityMemberResponse(
                member.getUserId(),
                username,
                member.getRole(),
                member.getStatus(),
                circle.getId(),
                circle.getName(),
                circle.getSlug(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getProfileBackgroundUrl(),
                profile.getProfileBackgroundType(),
                profile.getSkills(),
                profile.getGithubUrl(),
                profile.getWebsiteUrl(),
                0,
                0,
                false
        );
    }
}
