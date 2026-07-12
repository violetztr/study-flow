package com.studyflow.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MediaTranscodeTaskPublisher {
    private static final Logger log = LoggerFactory.getLogger(MediaTranscodeTaskPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaTaskQueueProperties properties;

    public MediaTranscodeTaskPublisher(
            RabbitTemplate rabbitTemplate,
            ApplicationEventPublisher eventPublisher,
            MediaTaskQueueProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    public void publishTranscodeRequested(Long mediaFileId) {
        if (!properties.isEnabled()) {
            publishLocalEvent(mediaFileId);
            return;
        }

        try {
            rabbitTemplate.convertAndSend(
                    properties.getExchange(),
                    properties.getTranscodeRoutingKey(),
                    new MediaTranscodeTaskMessage(mediaFileId)
            );
        } catch (AmqpException exception) {
            if (!properties.isFallbackToLocalEvent()) {
                throw exception;
            }
            log.warn(
                    "RabbitMQ transcode task publish failed, fallback to local event, mediaFileId={}, reason={}",
                    mediaFileId,
                    exception.getMessage()
            );
            publishLocalEvent(mediaFileId);
        }
    }

    private void publishLocalEvent(Long mediaFileId) {
        eventPublisher.publishEvent(new MediaTranscodeRequestedEvent(mediaFileId));
    }
}
