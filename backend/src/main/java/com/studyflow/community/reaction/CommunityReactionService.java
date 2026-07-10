package com.studyflow.community.reaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.wallet.PigWalletService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityReactionService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String TARGET_POST = "POST";
    private static final String REACTION_LIKE = "LIKE";
    private static final String REACTION_PIG = "PIG";

    private final CommunityReactionMapper communityReactionMapper;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityMemberService communityMemberService;
    private final PigWalletService pigWalletService;

    public CommunityReactionService(
            CommunityReactionMapper communityReactionMapper,
            CommunityPostMapper communityPostMapper,
            CommunityMemberService communityMemberService,
            PigWalletService pigWalletService
    ) {
        this.communityReactionMapper = communityReactionMapper;
        this.communityPostMapper = communityPostMapper;
        this.communityMemberService = communityMemberService;
        this.pigWalletService = pigWalletService;
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requirePublishedPost(circle.getId(), postId);
        if (hasReaction(circle.getId(), userId, post.getId(), REACTION_LIKE)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        CommunityReaction reaction = new CommunityReaction();
        reaction.setCircleId(circle.getId());
        reaction.setTargetType(TARGET_POST);
        reaction.setTargetId(post.getId());
        reaction.setUserId(userId);
        reaction.setReactionType(REACTION_LIKE);
        reaction.setCreatedAt(now);

        try {
            communityReactionMapper.insert(reaction);
        } catch (DuplicateKeyException ex) {
            return;
        }
        incrementPostReactionCount(post.getId(), now);
    }

    @Transactional
    public void pigPost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requirePublishedPost(circle.getId(), postId);
        if (hasReaction(circle.getId(), userId, post.getId(), REACTION_PIG)) {
            return;
        }

        pigWalletService.spendPig(userId, 1);

        LocalDateTime now = LocalDateTime.now();
        CommunityReaction reaction = new CommunityReaction();
        reaction.setCircleId(circle.getId());
        reaction.setTargetType(TARGET_POST);
        reaction.setTargetId(post.getId());
        reaction.setUserId(userId);
        reaction.setReactionType(REACTION_PIG);
        reaction.setCreatedAt(now);

        try {
            communityReactionMapper.insert(reaction);
        } catch (DuplicateKeyException ex) {
            return;
        }
        incrementPostPigCount(post.getId(), now);
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = requirePublishedPost(circle.getId(), postId);

        int deleted = communityReactionMapper.delete(new LambdaQueryWrapper<CommunityReaction>()
                .eq(CommunityReaction::getCircleId, circle.getId())
                .eq(CommunityReaction::getTargetType, TARGET_POST)
                .eq(CommunityReaction::getTargetId, post.getId())
                .eq(CommunityReaction::getUserId, userId)
                .eq(CommunityReaction::getReactionType, REACTION_LIKE));
        if (deleted == 0) {
            return;
        }
        decrementPostReactionCount(post.getId(), LocalDateTime.now());
    }

    public boolean hasLikedPost(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        Circle circle = communityMemberService.getDefaultCircle();
        return hasReaction(circle.getId(), userId, postId, REACTION_LIKE);
    }

    public Set<Long> likedPostIds(Long userId, Collection<Long> postIds) {
        if (userId == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        Circle circle = communityMemberService.getDefaultCircle();
        return communityReactionMapper.selectList(new LambdaQueryWrapper<CommunityReaction>()
                        .select(CommunityReaction::getTargetId)
                        .eq(CommunityReaction::getCircleId, circle.getId())
                        .eq(CommunityReaction::getTargetType, TARGET_POST)
                        .in(CommunityReaction::getTargetId, postIds)
                        .eq(CommunityReaction::getUserId, userId)
                        .eq(CommunityReaction::getReactionType, REACTION_LIKE))
                .stream()
                .map(CommunityReaction::getTargetId)
                .collect(Collectors.toSet());
    }

    public Set<Long> piggedPostIds(Long userId, Collection<Long> postIds) {
        if (userId == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        Circle circle = communityMemberService.getDefaultCircle();
        return communityReactionMapper.selectList(new LambdaQueryWrapper<CommunityReaction>()
                        .select(CommunityReaction::getTargetId)
                        .eq(CommunityReaction::getCircleId, circle.getId())
                        .eq(CommunityReaction::getTargetType, TARGET_POST)
                        .in(CommunityReaction::getTargetId, postIds)
                        .eq(CommunityReaction::getUserId, userId)
                        .eq(CommunityReaction::getReactionType, REACTION_PIG))
                .stream()
                .map(CommunityReaction::getTargetId)
                .collect(Collectors.toSet());
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

    private boolean hasReaction(Long circleId, Long userId, Long postId, String reactionType) {
        Long count = communityReactionMapper.selectCount(new LambdaQueryWrapper<CommunityReaction>()
                .eq(CommunityReaction::getCircleId, circleId)
                .eq(CommunityReaction::getTargetType, TARGET_POST)
                .eq(CommunityReaction::getTargetId, postId)
                .eq(CommunityReaction::getUserId, userId)
                .eq(CommunityReaction::getReactionType, reactionType));
        return count > 0;
    }

    private void incrementPostReactionCount(Long postId, LocalDateTime now) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("reaction_count = reaction_count + 1")
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(404, "帖子不存在");
        }
    }

    private void decrementPostReactionCount(Long postId, LocalDateTime now) {
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("reaction_count = CASE WHEN reaction_count > 0 THEN reaction_count - 1 ELSE 0 END")
                .set(CommunityPost::getUpdatedAt, now));
    }

    private void incrementPostPigCount(Long postId, LocalDateTime now) {
        int updated = communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .setSql("pig_count = pig_count + 1")
                .set(CommunityPost::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(404, "帖子不存在");
        }
    }
}
