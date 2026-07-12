package com.studyflow.media;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class MediaTaskQueueConfig {
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "study-flow.media.queue", name = "enabled", havingValue = "true")
    public DirectExchange mediaTaskExchange(MediaTaskQueueProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    @ConditionalOnProperty(prefix = "study-flow.media.queue", name = "enabled", havingValue = "true")
    public Queue mediaTranscodeQueue(MediaTaskQueueProperties properties) {
        return new Queue(properties.getTranscodeQueue(), true);
    }

    @Bean
    @ConditionalOnProperty(prefix = "study-flow.media.queue", name = "enabled", havingValue = "true")
    public Binding mediaTranscodeBinding(
            Queue mediaTranscodeQueue,
            DirectExchange mediaTaskExchange,
            MediaTaskQueueProperties properties
    ) {
        return BindingBuilder.bind(mediaTranscodeQueue)
                .to(mediaTaskExchange)
                .with(properties.getTranscodeRoutingKey());
    }
}
