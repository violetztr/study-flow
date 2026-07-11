package com.studyflow.community.member;

import com.studyflow.community.member.dto.CommunityMemberProfileResponse;
import com.studyflow.community.member.dto.CommunityMemberResponse;
import com.studyflow.community.post.CommunityPostService;
import com.studyflow.community.post.dto.CommunityPostResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CommunityMemberProfileService {
    private static final String CONTENT_TYPE_ARTICLE = "ARTICLE";
    private static final String CONTENT_TYPE_VIDEO = "VIDEO";
    private static final String CONTENT_TYPE_LIVE = "LIVE";

    private final CommunityMemberService communityMemberService;
    private final CommunityPostService communityPostService;

    public CommunityMemberProfileService(
            CommunityMemberService communityMemberService,
            CommunityPostService communityPostService
    ) {
        this.communityMemberService = communityMemberService;
        this.communityPostService = communityPostService;
    }

    @Transactional(readOnly = true)
    public CommunityMemberProfileResponse getProfile(Long currentUserId, Long targetUserId) {
        CommunityMemberResponse member = communityMemberService.getPublicMember(currentUserId, targetUserId);
        List<CommunityPostResponse> posts = communityPostService.listAuthorPosts(currentUserId, targetUserId);
        boolean currentUserProfile = currentUserId != null && currentUserId.equals(targetUserId);
        List<CommunityPostResponse> favoritePosts = currentUserProfile
                ? communityPostService.listFavoritePosts(currentUserId)
                : Collections.emptyList();

        return new CommunityMemberProfileResponse(
                member,
                posts,
                favoritePosts,
                posts.size(),
                countPostsByType(posts, CONTENT_TYPE_ARTICLE),
                countPostsByType(posts, CONTENT_TYPE_VIDEO),
                countPostsByType(posts, CONTENT_TYPE_LIVE),
                currentUserProfile
        );
    }

    private Integer countPostsByType(List<CommunityPostResponse> posts, String contentType) {
        return Math.toIntExact(posts.stream()
                .filter(post -> contentType.equals(post.contentType()))
                .count());
    }
}
