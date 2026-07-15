package com.studyflow.live;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class LiveWebSocketController {
    private static final Logger log = LoggerFactory.getLogger(LiveWebSocketController.class);

    private final LiveMessageService liveMessageService;

    public LiveWebSocketController(LiveMessageService liveMessageService) {
        this.liveMessageService = liveMessageService;
    }

    @MessageMapping("/live/{roomId}/chat")
    public void handleChat(@DestinationVariable Long roomId,
                           @Valid @Payload LiveMessageRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        String username = extractUsername(headerAccessor);
        liveMessageService.sendMessage(roomId, userId, username, request);
        log.debug("Chat message: roomId={}, userId={}", roomId, userId);
    }

    @MessageMapping("/live/{roomId}/danmaku")
    public void handleDanmaku(@DestinationVariable Long roomId,
                              @Valid @Payload LiveMessageRequest request,
                              SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        String username = extractUsername(headerAccessor);
        LiveMessageRequest danmakuRequest = new LiveMessageRequest(
                request.content(), request.color(), "DANMAKU"
        );
        liveMessageService.sendMessage(roomId, userId, username, danmakuRequest);
        log.debug("Danmaku message: roomId={}, userId={}", roomId, userId);
    }

    private Long extractUserId(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
            Object userId = sessionAttributes.get("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        throw new IllegalStateException("Unauthenticated");
    }

    private String extractUsername(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
            Object username = sessionAttributes.get("username");
            if (username instanceof String) {
                return (String) username;
            }
        }
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            return principal.getName();
        }
        return "anonymous";
    }
}
