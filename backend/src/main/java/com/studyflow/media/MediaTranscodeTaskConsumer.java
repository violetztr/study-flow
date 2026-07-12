package com.studyflow.media;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "study-flow.media.queue", name = "enabled", havingValue = "true")
public class MediaTranscodeTaskConsumer {
    private final MediaTranscodeService mediaTranscodeService;

    public MediaTranscodeTaskConsumer(MediaTranscodeService mediaTranscodeService) {
        this.mediaTranscodeService = mediaTranscodeService;
    }

    @RabbitListener(queues = "${study-flow.media.queue.transcode-queue:ruru.media.transcode}")
    public void handleTranscodeRequested(MediaTranscodeTaskMessage message) {
        mediaTranscodeService.transcodeVideo(message.mediaFileId());
    }
}
