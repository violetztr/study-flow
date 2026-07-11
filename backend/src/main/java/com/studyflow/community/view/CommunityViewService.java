package com.studyflow.community.view;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.community.post.CommunityPostService;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.community.view.dto.CommunityViewReportRequest;
import com.studyflow.community.view.dto.CommunityViewReportResponse;
import com.studyflow.community.view.dto.CommunityWatchHistoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityViewService {
    private static final String CONTENT_TYPE_VIDEO = "VIDEO";

    private final CommunityPostViewMapper communityPostViewMapper;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityPostService communityPostService;
    private final CommunityMemberService communityMemberService;

    public CommunityViewService(
            CommunityPostViewMapper communityPostViewMapper,
            CommunityPostMapper communityPostMapper,
            CommunityPostService communityPostService,
            CommunityMemberService communityMemberService
    ) {
        this.communityPostViewMapper = communityPostViewMapper;
        this.communityPostMapper = communityPostMapper;
        this.communityPostService = communityPostService;
        this.communityMemberService = communityMemberService;
    }

    @Transactional
    public CommunityViewReportResponse reportPlayback(
            Long userId,
            String viewerKey,
            Long postId,
            CommunityViewReportRequest request
    ) {
        Circle circle = communityMemberService.getDefaultCircle();
        CommunityPost post = communityPostService.requirePublishedPost(circle.getId(), postId);
        if (!CONTENT_TYPE_VIDEO.equals(post.getContentType())) {
            throw new BusinessException(400, "Only video playback can be reported");
        }

        int playedSeconds = safeSeconds(request.playedSeconds());
        int durationSeconds = safeSeconds(request.durationSeconds());
        boolean qualified = isQualifiedPlayback(playedSeconds, durationSeconds);
        LocalDateTime now = LocalDateTime.now();

        CommunityPostView view = findView(circle.getId(), postId, viewerKey);
        boolean countedNow = false;
        if (view == null) {
            view = new CommunityPostView();
            view.setCircleId(circle.getId());
            view.setPostId(postId);
            view.setUserId(userId);
            view.setViewerKey(viewerKey);
            view.setMaxProgressSeconds(playedSeconds);
            view.setDurationSeconds(durationSeconds);
            view.setCounted(qualified);
            view.setFirstViewedAt(now);
            view.setLastViewedAt(now);
            view.setCreatedAt(now);
            view.setUpdatedAt(now);
            communityPostViewMapper.insert(view);
            countedNow = qualified;
        } else {
            countedNow = qualified && !Boolean.TRUE.equals(view.getCounted());
            view.setUserId(userId == null ? view.getUserId() : userId);
            view.setMaxProgressSeconds(Math.max(view.getMaxProgressSeconds(), playedSeconds));
            view.setDurationSeconds(Math.max(view.getDurationSeconds(), durationSeconds));
            view.setCounted(Boolean.TRUE.equals(view.getCounted()) || qualified);
            view.setLastViewedAt(now);
            view.setUpdatedAt(now);
            communityPostViewMapper.updateById(view);
        }

        if (countedNow) {
            communityPostService.incrementViewCount(postId, now);
        }

        CommunityPost updatedPost = communityPostMapper.selectById(postId);
        return new CommunityViewReportResponse(countedNow, updatedPost.getViewCount());
    }

    public List<CommunityWatchHistoryResponse> listMyHistory(Long userId) {
        List<CommunityPostView> views = communityPostViewMapper.selectList(new LambdaQueryWrapper<CommunityPostView>()
                .eq(CommunityPostView::getUserId, userId)
                .orderByDesc(CommunityPostView::getLastViewedAt)
                .orderByDesc(CommunityPostView::getId)
                .last("LIMIT 50"));
        if (views.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = views.stream().map(CommunityPostView::getPostId).toList();
        Map<Long, CommunityPostResponse> postsById = communityPostService.listPublishedPostsByIds(userId, postIds)
                .stream()
                .collect(Collectors.toMap(CommunityPostResponse::id, Function.identity()));

        return views.stream()
                .filter(view -> postsById.containsKey(view.getPostId()))
                .map(view -> new CommunityWatchHistoryResponse(
                        postsById.get(view.getPostId()),
                        view.getMaxProgressSeconds(),
                        view.getDurationSeconds(),
                        view.getFirstViewedAt(),
                        view.getLastViewedAt()
                ))
                .toList();
    }

    private CommunityPostView findView(Long circleId, Long postId, String viewerKey) {
        return communityPostViewMapper.selectOne(new LambdaQueryWrapper<CommunityPostView>()
                .eq(CommunityPostView::getCircleId, circleId)
                .eq(CommunityPostView::getPostId, postId)
                .eq(CommunityPostView::getViewerKey, viewerKey));
    }

    private int safeSeconds(Integer seconds) {
        return seconds == null ? 0 : Math.max(0, seconds);
    }

    private boolean isQualifiedPlayback(int playedSeconds, int durationSeconds) {
        return playedSeconds >= 10 || (durationSeconds > 0 && playedSeconds * 5 >= durationSeconds);
    }
}
