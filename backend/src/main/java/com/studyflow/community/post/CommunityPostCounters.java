package com.studyflow.community.post;

public record CommunityPostCounters(
        Integer reactionCount,
        Integer pigCount,
        Integer favoriteCount,
        Integer viewCount
) {
    public static CommunityPostCounters fromPost(CommunityPost post) {
        return new CommunityPostCounters(
                safe(post.getReactionCount()),
                safe(post.getPigCount()),
                safe(post.getFavoriteCount()),
                safe(post.getViewCount())
        );
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
