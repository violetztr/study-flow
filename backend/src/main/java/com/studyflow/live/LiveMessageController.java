package com.studyflow.live;

import com.studyflow.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/live")
public class LiveMessageController {
    private final LiveMessageService liveMessageService;

    public LiveMessageController(LiveMessageService liveMessageService) {
        this.liveMessageService = liveMessageService;
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<List<LiveMessageResponse>> listRecentMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(liveMessageService.listRecentMessages(roomId, limit));
    }
}
