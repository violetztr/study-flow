package com.studyflow.live;

import com.studyflow.common.ApiResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/live")
public class LiveRoomController {
    private final LiveRoomService liveRoomService;
    private final LiveViewerService liveViewerService;

    public LiveRoomController(LiveRoomService liveRoomService, LiveViewerService liveViewerService) {
        this.liveRoomService = liveRoomService;
        this.liveViewerService = liveViewerService;
    }

    @PostMapping("/rooms")
    public ApiResponse<LiveRoomResponse> createLiveRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LiveRoomRequest request
    ) {
        return ApiResponse.success(liveRoomService.createLiveRoom(principal.userId(), request));
    }

    @GetMapping("/rooms")
    public ApiResponse<List<LiveRoomResponse>> listLiveRooms(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ApiResponse.success(List.of());
        }
        Long circleId = liveRoomService.defaultCircleId(principal.userId());
        return ApiResponse.success(liveRoomService.listLiveRooms(circleId));
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<LiveRoomResponse> getLiveRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId
    ) {
        Long currentUserId = principal != null ? principal.userId() : null;
        return ApiResponse.success(liveRoomService.getLiveRoom(roomId, currentUserId));
    }

    @PostMapping("/rooms/{roomId}/heartbeat")
    public ApiResponse<Void> heartbeat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId
    ) {
        liveViewerService.heartbeat(roomId, principal.userId());
        return ApiResponse.success(null);
    }
}
