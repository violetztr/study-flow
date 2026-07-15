package com.studyflow.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/live")
public class SrsCallbackController {
    private static final Logger log = LoggerFactory.getLogger(SrsCallbackController.class);

    private final LiveRoomService liveRoomService;

    public SrsCallbackController(LiveRoomService liveRoomService) {
        this.liveRoomService = liveRoomService;
    }

    @PostMapping("/srs/on_publish")
    public ResponseEntity<Integer> onPublish(@RequestBody Map<String, Object> body) {
        String stream = (String) body.get("stream");
        String app = (String) body.get("app");
        String ip = (String) body.get("client_id");
        log.info("SRS on_publish: app={}, stream={}, ip={}", app, stream, ip);

        if (stream == null || stream.isBlank()) {
            return ResponseEntity.ok(0);
        }

        try {
            liveRoomService.startLive(stream);
            return ResponseEntity.ok(0);
        } catch (Exception ex) {
            log.warn("SRS on_publish failed for stream={}: {}", stream, ex.getMessage());
            return ResponseEntity.ok(0);
        }
    }

    @PostMapping("/srs/on_unpublish")
    public ResponseEntity<Integer> onUnpublish(@RequestBody Map<String, Object> body) {
        String stream = (String) body.get("stream");
        String app = (String) body.get("app");
        log.info("SRS on_unpublish: app={}, stream={}", app, stream);

        if (stream == null || stream.isBlank()) {
            return ResponseEntity.ok(0);
        }

        try {
            liveRoomService.endLive(stream);
            return ResponseEntity.ok(0);
        } catch (Exception ex) {
            log.warn("SRS on_unpublish failed for stream={}: {}", stream, ex.getMessage());
            return ResponseEntity.ok(0);
        }
    }
}
