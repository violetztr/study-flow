package com.studyflow.community.favorite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostCounterService;
import com.studyflow.community.post.CommunityPostMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityFavoriteService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final CommunityFavoriteMapper communityFavoriteMapper;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityMemberService communityMemberService;
    private final CommunityPostCounterService communityPostCounterService;

    public CommunityFavoriteService(
            CommunityFavoriteMapper communityFavoriteMapper,
            CommunityPostMapper communityPostMapper,
            CommunityMemberService communityMemberService,
            CommunityPostCounterService communityPostCounterService
    ) {
        this.communityFavoriteMapper = communityFavoriteMapper;
        this.communityPostMapper = communityPostMapper;
        this.communityMemberService = communityMemberService;
        this.communityPostCounterService = communityPostCounterService;
    }

    @Transactional
    public void favoritePost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requirePublishedPost(circle.getId(), postId);
        if (hasFavorite(circle.getId(), userId, post.getId())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        CommunityFavorite favorite = new CommunityFavorite();
        favorite.setCircleId(circle.getId());
        favorite.setPostId(post.getId());
        favorite.setUserId(userId);
        favorite.setCreatedAt(now);

        try {
            communityFavoriteMapper.insert(favorite);
        } catch (DuplicateKeyException ex) {
            return;
        }
        incrementPostFavoriteCount(post.getId(), now);
    }

    @Transactional
    public void unfavoritePost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requirePublishedPost(circle.getId(), postId);

        int deleted = communityFavoriteMapper.delete(new LambdaQueryWrapper<CommunityFavorite>()
                .eq(CommunityFavorite::getCircleId, circle.getId())
                .eq(CommunityFavorite::getPostId, post.getId())
                .eq(CommunityFavorite::getUserId, userId));
        if (deleted == 0) {
            return;
        }
        decrementPostFavoriteCount(post.getId(), LocalDateTime.now());
    }

    public Set<Long> favoritedPostIds(Long userId, Collection<Long> postIds) {
        if (userId == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        Circle circle = communityMemberService.getDefaultCircle();
        return communityFavoriteMapper.selectList(new LambdaQueryWrapper<CommunityFavorite>()
                        .select(CommunityFavorite::getPostId)
                        .eq(CommunityFavorite::getCircleId, circle.getId())
                        .in(CommunityFavorite::getPostId, postIds)
                        .eq(CommunityFavorite::getUserId, userId))
                .stream()
                .map(CommunityFavorite::getPostId)
                .collect(Collectors.toSet());
    }

    public List<Long> favoritePostIdsByUser(Long userId) {
        Circle circle = communityMemberService.getDefaultCircle();
        return communityFavoriteMapper.selectList(new LambdaQueryWrapper<CommunityFavorite>()
                        .select(CommunityFavorite::getPostId)
                        .eq(CommunityFavorite::getCircleId, circle.getId())
                        .eq(CommunityFavorite::getUserId, userId)
                        .orderByDesc(CommunityFavorite::getCreatedAt)
                        .orderByDesc(CommunityFavorite::getId))
                .stream()
                .map(CommunityFavorite::getPostId)
                .toList();
    }

    private CommunityPost requirePublishedPost(Long circleId, Long postId) {
        CommunityPost post = communityPostMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getCircleId, circleId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED));
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        return post;
    }

    private boolean hasFavorite(Long circleId, Long userId, Long postId) {
        Long count = communityFavoriteMapper.selectCount(new LambdaQueryWrapper<CommunityFavorite>()
                .eq(CommunityFavorite::getCircleId, circleId)
                .eq(CommunityFavorite::getPostId, postId)
                .eq(CommunityFavorite::getUserId, userId));
        return count > 0;
    }

    private void incrementPostFavoriteCount(Long postId, LocalDateTime now) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("favorite_count = favorite_count + 1")
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(404, "帖子不存在");
        }
        communityPostCounterService.refreshPostCounter(postId);
    }

    private void decrementPostFavoriteCount(Long postId, LocalDateTime now) {
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END")
                .set(CommunityPost::getUpdatedAt, now));
        communityPostCounterService.refreshPostCounter(postId);
    }
}
