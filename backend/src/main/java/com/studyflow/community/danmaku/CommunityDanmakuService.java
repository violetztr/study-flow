package com.studyflow.community.danmaku;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.danmaku.dto.CommunityDanmakuRequest;
import com.studyflow.community.danmaku.dto.CommunityDanmakuResponse;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.member.UserProfile;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostService;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityDanmakuService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String CONTENT_TYPE_VIDEO = "VIDEO";
    private static final String DEFAULT_COLOR = "#ffffff";

    private final CommunityDanmakuMapper communityDanmakuMapper;
    private final CommunityMemberService communityMemberService;
    private final CommunityPostService communityPostService;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    public CommunityDanmakuService(
            CommunityDanmakuMapper communityDanmakuMapper,
            CommunityMemberService communityMemberService,
            CommunityPostService communityPostService,
            UserProfileMapper userProfileMapper,
            UserMapper userMapper
    ) {
        this.communityDanmakuMapper = communityDanmakuMapper;
        this.communityMemberService = communityMemberService;
        this.communityPostService = communityPostService;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
    }

    public List<CommunityDanmakuResponse> listDanmaku(Long postId) {
        Circle circle = communityMemberService.getDefaultCircle();
        communityPostService.requirePublishedPost(circle.getId(), postId);
        List<CommunityDanmaku> danmaku = communityDanmakuMapper.selectList(new LambdaQueryWrapper<CommunityDanmaku>()
                .eq(CommunityDanmaku::getPostId, postId)
                .eq(CommunityDanmaku::getStatus, STATUS_PUBLISHED)
                .orderByAsc(CommunityDanmaku::getTimeSeconds)
                .orderByAsc(CommunityDanmaku::getId));
        return toResponses(danmaku);
    }

    @Transactional
    public CommunityDanmakuResponse createDanmaku(Long userId, Long postId, CommunityDanmakuRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        CommunityPost post = communityPostService.requirePublishedPost(circle.getId(), postId);
        if (!CONTENT_TYPE_VIDEO.equals(post.getContentType())) {
            throw new BusinessException(400, "Only video posts can receive danmaku");
        }

        CommunityDanmaku danmaku = new CommunityDanmaku();
        danmaku.setPostId(post.getId());
        danmaku.setUserId(userId);
        danmaku.setContent(request.content());
        danmaku.setTimeSeconds(request.timeSeconds());
        danmaku.setColor(request.color() == null || request.color().isBlank() ? DEFAULT_COLOR : request.color());
        danmaku.setStatus(STATUS_PUBLISHED);
        danmaku.setCreatedAt(LocalDateTime.now());
        communityDanmakuMapper.insert(danmaku);
        return toResponse(danmaku);
    }

    private List<CommunityDanmakuResponse> toResponses(List<CommunityDanmaku> danmaku) {
        if (danmaku.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = danmaku.stream()
                .map(CommunityDanmaku::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> authorNames = authorNames(authorIds);
        return danmaku.stream()
                .map(item -> toResponse(item, authorNames))
                .toList();
    }

    private CommunityDanmakuResponse toResponse(CommunityDanmaku danmaku) {
        return toResponses(List.of(danmaku)).get(0);
    }

    private CommunityDanmakuResponse toResponse(CommunityDanmaku danmaku, Map<Long, String> authorNames) {
        return new CommunityDanmakuResponse(
                danmaku.getId(),
                danmaku.getPostId(),
                danmaku.getUserId(),
                authorNames.getOrDefault(danmaku.getUserId(), ""),
                danmaku.getContent(),
                danmaku.getTimeSeconds(),
                danmaku.getColor(),
                danmaku.getCreatedAt()
        );
    }

    private Map<Long, String> authorNames(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, authorIds))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getUserId, authorIds))
                .forEach(profile -> {
                    if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
                        names.put(profile.getUserId(), profile.getDisplayName());
                    }
                });
        return names;
    }
}
