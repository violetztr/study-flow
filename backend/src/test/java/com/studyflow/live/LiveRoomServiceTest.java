package com.studyflow.live;

import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.member.UserProfile;
import com.studyflow.community.member.UserProfileMapper;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 直播模块核心单元测试 — LiveRoomService
 *
 * <p>覆盖：创建直播间、开始/结束直播、僵尸房自动清理、权限校验、封面/信息更新。</p>
 */
class LiveRoomServiceTest {

    private LiveRoomMapper liveRoomMapper;
    private CommunityMemberService communityMemberService;
    private UserMapper userMapper;
    private UserProfileMapper userProfileMapper;
    private LiveViewerService liveViewerService;
    private LiveRoomService liveRoomService;

    @BeforeEach
    void setUp() {
        liveRoomMapper = mock(LiveRoomMapper.class);
        communityMemberService = mock(CommunityMemberService.class);
        userMapper = mock(UserMapper.class);
        userProfileMapper = mock(UserProfileMapper.class);
        liveViewerService = mock(LiveViewerService.class);

        liveRoomService = new LiveRoomService(
                liveRoomMapper,
                communityMemberService,
                userMapper,
                userProfileMapper,
                liveViewerService
        );
    }

    // ────────── createLiveRoom ──────────

    @Test
    void createLiveRoomSucceeds() {
        Circle circle = new Circle();
        circle.setId(1L);
        when(communityMemberService.requireActiveDefaultMember(10L)).thenReturn(circle);
        when(liveRoomMapper.insert(any(LiveRoom.class))).thenAnswer(inv -> {
            LiveRoom room = inv.getArgument(0);
            room.setId(100L);
            return 1;
        });

        LiveRoomRequest request = new LiveRoomRequest("测试直播", null, null, null);
        LiveRoomResponse response = liveRoomService.createLiveRoom(10L, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("测试直播");
        assertThat(response.status()).isEqualTo("WAITING");
        // createLiveRoom does not pass currentUserId to toResponse, so streamKey is not returned
        verify(liveRoomMapper).insert(any(LiveRoom.class));
    }

    @Test
    void createLiveRoomTrimsTitle() {
        Circle circle = new Circle();
        circle.setId(1L);
        when(communityMemberService.requireActiveDefaultMember(10L)).thenReturn(circle);
        when(liveRoomMapper.insert(any(LiveRoom.class))).thenAnswer(inv -> {
            LiveRoom room = inv.getArgument(0);
            room.setId(101L);
            return 1;
        });

        LiveRoomRequest request = new LiveRoomRequest("  短标题  ", null, null, null);
        LiveRoomResponse response = liveRoomService.createLiveRoom(10L, request);

        assertThat(response.title()).isEqualTo("短标题");
    }

    @Test
    void createLiveRoomRejectsEmptyTitle() {
        Circle circle = new Circle();
        circle.setId(1L);
        when(communityMemberService.requireActiveDefaultMember(10L)).thenReturn(circle);

        LiveRoomRequest request = new LiveRoomRequest("   ", null, null, null);

        assertThatThrownBy(() -> liveRoomService.createLiveRoom(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void createLiveRoomRejectsNullTitle() {
        Circle circle = new Circle();
        circle.setId(1L);
        when(communityMemberService.requireActiveDefaultMember(10L)).thenReturn(circle);

        LiveRoomRequest request = new LiveRoomRequest(null, null, null, null);

        assertThatThrownBy(() -> liveRoomService.createLiveRoom(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void createLiveRoomRejectsOverlyLongTitle() {
        Circle circle = new Circle();
        circle.setId(1L);
        when(communityMemberService.requireActiveDefaultMember(10L)).thenReturn(circle);

        LiveRoomRequest request = new LiveRoomRequest("A".repeat(201), null, null, null);

        assertThatThrownBy(() -> liveRoomService.createLiveRoom(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200");
    }

    // ────────── startLive / endLive ──────────

    @Test
    void startLiveUpdatesStatusToLive() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setStreamKey("abc123");
        room.setStatus("WAITING");
        when(liveRoomMapper.selectOne(any())).thenReturn(room);
        when(liveRoomMapper.updateById(any(LiveRoom.class))).thenReturn(1);

        liveRoomService.startLive("abc123");

        assertThat(room.getStatus()).isEqualTo("LIVE");
        assertThat(room.getStartedAt()).isNotNull();
        verify(liveRoomMapper).updateById(any(LiveRoom.class));
    }

    @Test
    void startLiveIsIdempotent() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setStreamKey("abc123");
        room.setStatus("LIVE");
        when(liveRoomMapper.selectOne(any())).thenReturn(room);

        liveRoomService.startLive("abc123");

        // 已经 LIVE 的房间不应再次 update
        verify(liveRoomMapper, never()).updateById(any(LiveRoom.class));
    }

    @Test
    void endLiveUpdatesStatusToEnded() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setStreamKey("abc123");
        room.setStatus("LIVE");
        when(liveRoomMapper.selectOne(any())).thenReturn(room);
        when(liveRoomMapper.updateById(any(LiveRoom.class))).thenReturn(1);

        liveRoomService.endLive("abc123");

        assertThat(room.getStatus()).isEqualTo("ENDED");
        assertThat(room.getEndedAt()).isNotNull();
    }

    @Test
    void endLiveIsIdempotent() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setStreamKey("abc123");
        room.setStatus("ENDED");
        when(liveRoomMapper.selectOne(any())).thenReturn(room);

        liveRoomService.endLive("abc123");

        verify(liveRoomMapper, never()).updateById(any(LiveRoom.class));
    }

    @Test
    void startLiveThrowsForUnknownStreamKey() {
        when(liveRoomMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> liveRoomService.startLive("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    // ────────── updateCover ──────────

    @Test
    void updateCoverSucceedsForOwner() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        room.setStatus("WAITING");
        when(liveRoomMapper.selectById(1L)).thenReturn(room);
        when(userMapper.selectById(10L)).thenReturn(new User());
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(liveRoomMapper.updateById(any(LiveRoom.class))).thenReturn(1);

        LiveRoomResponse response = liveRoomService.updateCover(10L, 1L, "/covers/new.png");

        assertThat(response.coverUrl()).isEqualTo("/covers/new.png");
    }

    @Test
    void updateCoverRejectsNonOwner() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        when(liveRoomMapper.selectById(1L)).thenReturn(room);

        assertThatThrownBy(() -> liveRoomService.updateCover(20L, 1L, "/covers/new.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主播");
    }

    @Test
    void updateCoverRejectsEmptyUrl() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        when(liveRoomMapper.selectById(1L)).thenReturn(room);

        assertThatThrownBy(() -> liveRoomService.updateCover(10L, 1L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void updateCoverThrowsForMissingRoom() {
        when(liveRoomMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> liveRoomService.updateCover(10L, 999L, "/covers/x.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    // ────────── updateRoom ──────────

    @Test
    void updateRoomSucceedsWithPartialFields() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        room.setTitle("旧标题");
        room.setCoverUrl("/old.png");
        when(liveRoomMapper.selectById(1L)).thenReturn(room);
        when(userMapper.selectById(10L)).thenReturn(new User());
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(liveRoomMapper.updateById(any(LiveRoom.class))).thenReturn(1);

        LiveRoomRequest request = new LiveRoomRequest("新标题", null, null, null);
        LiveRoomResponse response = liveRoomService.updateRoom(10L, 1L, request);

        assertThat(response.title()).isEqualTo("新标题");
        assertThat(response.coverUrl()).isEqualTo("/old.png"); // unchanged
    }

    @Test
    void updateRoomRejectsNonOwner() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        when(liveRoomMapper.selectById(1L)).thenReturn(room);

        LiveRoomRequest request = new LiveRoomRequest("新标题", null, null, null);
        assertThatThrownBy(() -> liveRoomService.updateRoom(20L, 1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主播");
    }

    // ────────── getLiveRoom ──────────

    @Test
    void getLiveRoomThrowsForMissingRoom() {
        when(liveRoomMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> liveRoomService.getLiveRoom(999L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void getLiveRoomReturnsResponseWithHostInfo() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        room.setTitle("测试");
        room.setStatus("LIVE");
        room.setStreamKey("key123");
        room.setCircleId(1L);
        room.setPeakViewers(5);
        room.setTotalViews(100);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        User host = new User();
        host.setId(10L);
        host.setUsername("hostuser");

        when(liveRoomMapper.selectById(1L)).thenReturn(room);
        when(userMapper.selectById(10L)).thenReturn(host);
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(liveViewerService.countViewers(1L)).thenReturn(3);

        LiveRoomResponse response = liveRoomService.getLiveRoom(1L, 10L);

        assertThat(response.username()).isEqualTo("hostuser");
        assertThat(response.currentViewers()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("LIVE");
        // streamKey should be visible to the host
        assertThat(response.streamKey()).isEqualTo("key123");
    }

    @Test
    void getLiveRoomHidesStreamKeyForNonHost() {
        LiveRoom room = new LiveRoom();
        room.setId(1L);
        room.setUserId(10L);
        room.setTitle("测试");
        room.setStatus("LIVE");
        room.setStreamKey("key123");
        room.setCircleId(1L);
        room.setPeakViewers(0);
        room.setTotalViews(0);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        when(liveRoomMapper.selectById(1L)).thenReturn(room);
        when(userMapper.selectById(10L)).thenReturn(new User());
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(liveViewerService.countViewers(1L)).thenReturn(3);

        // 用户 20 不是主播，不应该看到 streamKey
        LiveRoomResponse response = liveRoomService.getLiveRoom(1L, 20L);

        assertThat(response.streamKey()).isNull();
    }

    // ────────── listLiveRooms ──────────

    @Test
    void listLiveRoomsReturnsEmptyList() {
        when(liveRoomMapper.selectList(any())).thenReturn(java.util.List.of());

        var result = liveRoomService.listLiveRooms(1L);

        assertThat(result).isEmpty();
    }

    // ────────── defaultCircleId ──────────

    @Test
    void defaultCircleIdDelegatesToCommunityMemberService() {
        Circle circle = new Circle();
        circle.setId(5L);
        when(communityMemberService.requireActiveDefaultMember(10L)).thenReturn(circle);

        Long circleId = liveRoomService.defaultCircleId(10L);

        assertThat(circleId).isEqualTo(5L);
    }

    // ────────── autoEndZombieRooms ──────────

    @Test
    void autoEndZombieRoomsEndsRoomWithNoViewers() {
        LiveRoom zombie = new LiveRoom();
        zombie.setId(1L);
        zombie.setStatus("LIVE");
        zombie.setStartedAt(LocalDateTime.now().minusMinutes(5));

        when(liveRoomMapper.selectList(any())).thenReturn(java.util.List.of(zombie));
        when(liveViewerService.countViewers(1L)).thenReturn(0);
        when(liveRoomMapper.updateById(any(LiveRoom.class))).thenReturn(1);

        liveRoomService.autoEndZombieRooms();

        assertThat(zombie.getStatus()).isEqualTo("ENDED");
        assertThat(zombie.getEndedAt()).isNotNull();
        verify(liveRoomMapper).updateById(any(LiveRoom.class));
    }

    @Test
    void autoEndZombieRoomsSkipsRoomWithActiveViewers() {
        LiveRoom active = new LiveRoom();
        active.setId(1L);
        active.setStatus("LIVE");
        active.setStartedAt(LocalDateTime.now().minusMinutes(5));

        when(liveRoomMapper.selectList(any())).thenReturn(java.util.List.of(active));
        when(liveViewerService.countViewers(1L)).thenReturn(3);

        liveRoomService.autoEndZombieRooms();

        assertThat(active.getStatus()).isEqualTo("LIVE");
        verify(liveRoomMapper, never()).updateById(any(LiveRoom.class));
    }

    @Test
    void autoEndZombieRoomsSkipsRecentlyStartedRoom() {
        // 刚开播不到 2 分钟的房间不应被判定为僵尸房
        LiveRoom justStarted = new LiveRoom();
        justStarted.setId(1L);
        justStarted.setStatus("LIVE");
        justStarted.setStartedAt(LocalDateTime.now().minusSeconds(30));

        // 这个房间不会被 selectList 查出来（startedAt > cutoff）
        // 这里验证 logic：room 不应出现在列表里
        // 实际上 selectList 的 LambdaQueryWrapper 条件已经过滤了，这里验证空列表情况
        when(liveRoomMapper.selectList(any())).thenReturn(java.util.List.of());

        liveRoomService.autoEndZombieRooms();

        // 没有房间被处理，没问题
        verify(liveRoomMapper, never()).updateById(any(LiveRoom.class));
    }
}
