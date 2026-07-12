package com.studyflow.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "study-flow.media.queue")
public class MediaTaskQueueProperties {
    private boolean enabled;
    private String exchange = "ruru.media";
    private String transcodeQueue = "ruru.media.transcode";
    private String transcodeRoutingKey = "media.transcode.requested";
    private boolean fallbackToLocalEvent = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getTranscodeQueue() {
        return transcodeQueue;
    }

    public void setTranscodeQueue(String transcodeQueue) {
        this.transcodeQueue = transcodeQueue;
    }

    public String getTranscodeRoutingKey() {
        return transcodeRoutingKey;
    }

    public void setTranscodeRoutingKey(String transcodeRoutingKey) {
        this.transcodeRoutingKey = transcodeRoutingKey;
    }

    public boolean isFallbackToLocalEvent() {
        return fallbackToLocalEvent;
    }

    public void setFallbackToLocalEvent(boolean fallbackToLocalEvent) {
        this.fallbackToLocalEvent = fallbackToLocalEvent;
    }
}
