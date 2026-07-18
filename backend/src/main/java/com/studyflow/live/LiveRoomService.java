package com.studyflow.live;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.community.member.UserProfile;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LiveRoomService {
    private static final Logger log = LoggerFactory.getLogger(LiveRoomService.class);

    private final LiveRoomMapper liveRoomMapper;
    private final CommunityMemberService communityMemberService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final LiveViewerService liveViewerService;

    public LiveRoomService(LiveRoomMapper liveRoomMapper,
                           CommunityMemberService communityMemberService,
                           UserMapper userMapper,
                           UserProfileMapper userProfileMapper,
                           LiveViewerService liveViewerService) {
        this.liveRoomMapper = liveRoomMapper;
        this.communityMemberService = communityMemberService;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.liveViewerService = liveViewerService;
    }

    @Transactional
    public LiveRoomResponse createLiveRoom(Long userId, LiveRoomRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        Long circleId = circle.getId();

        LiveRoom room = new LiveRoom();
        room.setUserId(userId);
        room.setCircleId(circleId);
        room.setTitle(normalizeTitle(request.title()));
        room.setCoverUrl(request.coverUrl());
        room.setTopicId(request.topicId());
        room.setTopicName(request.topicName());
        room.setStreamKey(generateStreamKey());
        room.setStatus("WAITING");
        room.setPeakViewers(0);
        room.setTotalViews(0);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        liveRoomMapper.insert(room);
        log.info("LiveRoom created: id={}, userId={}, streamKey={}", room.getId(), userId, room.getStreamKey());

        return toResponse(room, null, null, null);
    }

    @Transactional
    public void startLive(String streamKey) {
        LiveRoom room = requireByStreamKey(streamKey);
        if (!"LIVE".equals(room.getStatus())) {
            room.setStatus("LIVE");
            room.setStartedAt(LocalDateTime.now());
            room.setUpdatedAt(LocalDateTime.now());
            liveRoomMapper.updateById(room);
            log.info("LiveRoom started: id={}, streamKey={}", room.getId(), streamKey);
        }
    }

    @Transactional
    public void endLive(String streamKey) {
        LiveRoom room = requireByStreamKey(streamKey);
        if ("LIVE".equals(room.getStatus())) {
            room.setStatus("ENDED");
            room.setEndedAt(LocalDateTime.now());
            room.setUpdatedAt(LocalDateTime.now());
            liveRoomMapper.updateById(room);
            log.info("LiveRoom ended: id={}, streamKey={}", room.getId(), streamKey);
        }
    }

    @Transactional
    public LiveRoomResponse updateCover(Long userId, Long roomId, String coverUrl) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("直播间不存在");
        }
        if (!room.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只有主播可以修改封面");
        }
        if (coverUrl == null || coverUrl.isBlank()) {
            throw new IllegalArgumentException("封面地址不能为空");
        }
        room.setCoverUrl(coverUrl.trim());
        room.setUpdatedAt(LocalDateTime.now());
        liveRoomMapper.updateById(room);

        return getLiveRoom(roomId, userId);
    }

    public List<LiveRoomResponse> listLiveRooms(Long circleId) {
        List<LiveRoom> rooms = liveRoomMapper.selectList(
                new LambdaQueryWrapper<LiveRoom>()
                        .eq(LiveRoom::getCircleId, circleId)
                        .eq(LiveRoom::getStatus, "LIVE")
                        .orderByDesc(LiveRoom::getCreatedAt)
        );
        return rooms.stream()
                .map(room -> toResponse(room, null, null, null))
                .collect(Collectors.toList());
    }

    public LiveRoomResponse getLiveRoom(Long roomId, Long currentUserId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("直播间不存在");
        }

        User user = userMapper.selectById(room.getUserId());
        UserProfile profile = user != null
                ? userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, room.getUserId()))
                : null;

        String username = user != null ? user.getUsername() : null;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

        return toResponse(room, username, avatarUrl, currentUserId);
    }

    public Long defaultCircleId(Long userId) {
        return communityMemberService.requireActiveDefaultMember(userId).getId();
    }

    private LiveRoomResponse toResponse(LiveRoom room, String username, String avatarUrl, Long currentUserId) {
        boolean isOwner = currentUserId != null && currentUserId.equals(room.getUserId());
        boolean isLive = "LIVE".equals(room.getStatus());

        int currentViewers = 0;
        if (isLive) {
            currentViewers = liveViewerService.countViewers(room.getId());
            liveViewerService.updatePeakIfNeeded(room.getId(), currentViewers);
        }

        int peakViewers = room.getPeakViewers() != null ? room.getPeakViewers() : 0;

        return new LiveRoomResponse(
                room.getId(),
                room.getUserId(),
                username,
                avatarUrl,
                room.getCircleId(),
                room.getTitle(),
                room.getCoverUrl(),
                room.getTopicId(),
                room.getTopicName(),
                isOwner ? room.getStreamKey() : null,
                room.getStatus(),
                room.getStartedAt(),
                room.getEndedAt(),
                peakViewers,
                room.getTotalViews(),
                currentViewers,
                isLive ? "/live/" + room.getStreamKey() + ".flv" : null,
                isLive ? "/live/" + room.getStreamKey() + "/index.m3u8" : null,
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    private LiveRoom requireByStreamKey(String streamKey) {
        LiveRoom room = liveRoomMapper.selectOne(
                new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getStreamKey, streamKey)
        );
        if (room == null) {
            throw new IllegalArgumentException("直播间不存在: streamKey=" + streamKey);
        }
        return room;
    }

    private String generateStreamKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("直播标题不能为空");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("直播标题不能超过200个字");
        }
        return trimmed;
    }
}
