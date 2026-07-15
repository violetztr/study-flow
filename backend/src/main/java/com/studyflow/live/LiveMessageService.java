package com.studyflow.live;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LiveMessageService {
    private static final Logger log = LoggerFactory.getLogger(LiveMessageService.class);

    private final LiveMessageMapper liveMessageMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final CommunityMemberService communityMemberService;
    private final SimpMessagingTemplate messagingTemplate;

    public LiveMessageService(LiveMessageMapper liveMessageMapper,
                              LiveRoomMapper liveRoomMapper,
                              CommunityMemberService communityMemberService,
                              SimpMessagingTemplate messagingTemplate) {
        this.liveMessageMapper = liveMessageMapper;
        this.liveRoomMapper = liveRoomMapper;
        this.communityMemberService = communityMemberService;
        this.messagingTemplate = messagingTemplate;
    }

    public void sendMessage(Long roomId, Long userId, String username, LiveMessageRequest request) {
        Circle circle = communityMemberService.requireActiveDefaultMember(userId);
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || !room.getCircleId().equals(circle.getId())) {
            throw new IllegalArgumentException("直播间不存在");
        }

        LiveMessage message = new LiveMessage();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setContent(normalizeContent(request.content()));
        message.setColor(normalizeColor(request.color()));
        message.setType(request.type());
        message.setCreatedAt(LocalDateTime.now());

        liveMessageMapper.insert(message);
        log.debug("LiveMessage saved: id={}, roomId={}, userId={}, type={}", message.getId(), roomId, userId, message.getType());

        LiveMessageResponse response = new LiveMessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                username,
                message.getContent(),
                message.getColor(),
                message.getType(),
                message.getCreatedAt()
        );

        messagingTemplate.convertAndSend("/topic/live/" + roomId, response);
    }

    public List<LiveMessageResponse> listRecentMessages(Long roomId, int limit) {
        List<LiveMessage> messages = liveMessageMapper.selectList(
                new LambdaQueryWrapper<LiveMessage>()
                        .eq(LiveMessage::getRoomId, roomId)
                        .orderByDesc(LiveMessage::getCreatedAt)
                        .last("LIMIT " + Math.min(limit, 200))
        );

        return messages.stream()
                .map(msg -> new LiveMessageResponse(
                        msg.getId(),
                        msg.getRoomId(),
                        msg.getUserId(),
                        null,
                        msg.getContent(),
                        msg.getColor(),
                        msg.getType(),
                        msg.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        return content.trim();
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "#ffffff";
        }
        String trimmed = color.trim();
        if (!trimmed.matches("^#[0-9A-Fa-f]{6}$")) {
            return "#ffffff";
        }
        return trimmed;
    }
}
