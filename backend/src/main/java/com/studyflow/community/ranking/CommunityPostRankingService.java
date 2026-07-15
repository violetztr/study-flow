package com.studyflow.community.ranking;

import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostCounters;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.infrastructure.redis.RedisCacheService;
import com.studyflow.infrastructure.redis.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityPostRankingService {
    private static final Logger log = LoggerFactory.getLogger(CommunityPostRankingService.class);
    private static final Duration RANKING_TTL = Duration.ofDays(7);
    private static final int MAX_RANKING_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RedisCacheService redisCacheService;
    private final CommunityPostMapper communityPostMapper;

    public CommunityPostRankingService(
            RedisCacheService redisCacheService,
            CommunityPostMapper communityPostMapper
    ) {
        this.redisCacheService = redisCacheService;
        this.communityPostMapper = communityPostMapper;
    }

    public void updateRanking(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            removeRanking(postId);
            return;
        }
        double score = calculateScore(
                CommunityPostCounters.fromPost(post),
                post.getCommentCount()
        );
        String key = RedisKeys.hotRanking();
        try {
            var result = redisCacheService.zadd(key, String.valueOf(postId), score);
            if (result.isPresent()) {
                redisCacheService.zremrangeByRank(key, 0, -(MAX_RANKING_SIZE + 1));
            }
        } catch (Exception ex) {
            log.warn("Redis ranking update failed for post {}: {}", postId, ex.getMessage());
        }
    }

    public List<Long> getRankingIds(int page, int pageSize) {
        String key = RedisKeys.hotRanking();
        try {
            int start = (page - 1) * pageSize;
            int end = start + pageSize - 1;
            Optional<Set<String>> members = redisCacheService.zrevrange(key, start, end);
            if (members.isPresent() && !members.get().isEmpty()) {
                return members.get().stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            log.warn("Redis ranking read failed: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }

    public void rebuildRankings() {
        try {
            String key = RedisKeys.hotRanking();
            redisCacheService.zremrangeByRank(key, 0, -1);

            List<CommunityPost> posts = communityPostMapper.selectList(null);
            for (CommunityPost post : posts) {
                double score = calculateScore(
                        CommunityPostCounters.fromPost(post),
                        post.getCommentCount()
                );
                redisCacheService.zadd(key, String.valueOf(post.getId()), score);
            }
            redisCacheService.zremrangeByRank(key, 0, -(MAX_RANKING_SIZE + 1));
            log.info("Ranking rebuilt with {} posts", posts.size());
        } catch (Exception ex) {
            log.warn("Redis ranking rebuild failed: {}", ex.getMessage());
        }
    }

    private void removeRanking(Long postId) {
        try {
            redisCacheService.zrem(RedisKeys.hotRanking(), String.valueOf(postId));
        } catch (Exception ex) {
            log.warn("Redis ranking remove failed for post {}: {}", postId, ex.getMessage());
        }
    }

    static double calculateScore(CommunityPostCounters counters, Integer commentCount) {
        int viewCount = counters.viewCount() != null ? counters.viewCount() : 0;
        int reactionCount = counters.reactionCount() != null ? counters.reactionCount() : 0;
        int pigCount = counters.pigCount() != null ? counters.pigCount() : 0;
        int favoriteCount = counters.favoriteCount() != null ? counters.favoriteCount() : 0;
        int comments = commentCount != null ? commentCount : 0;

        return (double) viewCount
                + (double) reactionCount * 3
                + (double) comments * 2
                + (double) pigCount * 5
                + (double) favoriteCount * 4;
    }
}
