package com.studyflow.live;

import com.studyflow.infrastructure.redis.RedisCacheService;
import com.studyflow.infrastructure.redis.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class LiveViewerService {
    private static final Logger log = LoggerFactory.getLogger(LiveViewerService.class);

    private static final Duration VIEWER_TTL = Duration.ofSeconds(30);

    private final RedisCacheService redisCacheService;
    private final LiveRoomMapper liveRoomMapper;

    public LiveViewerService(RedisCacheService redisCacheService, LiveRoomMapper liveRoomMapper) {
        this.redisCacheService = redisCacheService;
        this.liveRoomMapper = liveRoomMapper;
    }

    /**
     * Record a heartbeat for a viewer in a live room.
     * Updates the ZSet score to the current timestamp (seconds).
     */
    public void heartbeat(Long roomId, Long userId) {
        String key = RedisKeys.liveViewers(roomId);
        double now = Instant.now().getEpochSecond();
        redisCacheService.zadd(key, String.valueOf(userId), now);
    }

    /**
     * Count current online viewers for a room by removing expired entries
     * and returning the size of the ZSet.
     */
    public int countViewers(Long roomId) {
        String key = RedisKeys.liveViewers(roomId);
        cleanupExpired(key);
        return redisCacheService.zcard(key).orElse(0L).intValue();
    }

    /**
     * Remove viewers whose last heartbeat is older than VIEWER_TTL.
     */
    private void cleanupExpired(String key) {
        double cutoff = Instant.now().getEpochSecond() - VIEWER_TTL.getSeconds();
        redisCacheService.zremrangeByScore(key, 0, cutoff);
    }

    /**
     * Periodically clean up stale viewers and update peak viewer counts.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedDelay = 30_000)
    public void scheduledCleanup() {
        // This is a lightweight scheduled task that runs even if no one is watching.
        // The actual peak update happens lazily when room data is fetched.
        log.debug("Live viewer cleanup tick");
    }

    /**
     * Update peak viewers for a room if current count exceeds stored peak.
     */
    public void updatePeakIfNeeded(Long roomId, int currentViewers) {
        if (currentViewers <= 0) return;

        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) return;

        int peak = room.getPeakViewers() != null ? room.getPeakViewers() : 0;
        if (currentViewers > peak) {
            room.setPeakViewers(currentViewers);
            liveRoomMapper.updateById(room);
        }
    }
}
