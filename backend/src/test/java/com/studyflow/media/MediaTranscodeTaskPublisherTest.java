package com.studyflow.media;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MediaTranscodeTaskPublisherTest {
    @Test
    void publishesRabbitMessageWhenQueueIsEnabled() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MediaTaskQueueProperties properties = enabledProperties();
        MediaTranscodeTaskPublisher publisher = new MediaTranscodeTaskPublisher(
                rabbitTemplate,
                eventPublisher,
                properties
        );

        publisher.publishTranscodeRequested(42L);

        verify(rabbitTemplate).convertAndSend(
                "ruru.media",
                "media.transcode.requested",
                new MediaTranscodeTaskMessage(42L)
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void fallsBackToLocalEventWhenQueueIsDisabled() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MediaTaskQueueProperties properties = enabledProperties();
        properties.setEnabled(false);
        MediaTranscodeTaskPublisher publisher = new MediaTranscodeTaskPublisher(
                rabbitTemplate,
                eventPublisher,
                properties
        );

        publisher.publishTranscodeRequested(42L);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(eventPublisher).publishEvent(new MediaTranscodeRequestedEvent(42L));
    }

    @Test
    void fallsBackToLocalEventWhenRabbitIsTemporarilyUnavailable() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MediaTaskQueueProperties properties = enabledProperties();
        MediaTranscodeTaskPublisher publisher = new MediaTranscodeTaskPublisher(
                rabbitTemplate,
                eventPublisher,
                properties
        );
        doThrow(new AmqpConnectException(new RuntimeException("rabbit down")))
                .when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(MediaTranscodeTaskMessage.class));

        publisher.publishTranscodeRequested(42L);

        verify(eventPublisher).publishEvent(new MediaTranscodeRequestedEvent(42L));
    }

    private MediaTaskQueueProperties enabledProperties() {
        MediaTaskQueueProperties properties = new MediaTaskQueueProperties();
        properties.setEnabled(true);
        properties.setExchange("ruru.media");
        properties.setTranscodeQueue("ruru.media.transcode");
        properties.setTranscodeRoutingKey("media.transcode.requested");
        properties.setFallbackToLocalEvent(true);
        return properties;
    }
}
