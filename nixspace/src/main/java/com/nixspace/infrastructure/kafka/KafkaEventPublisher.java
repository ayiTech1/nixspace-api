package com.nixspace.infrastructure.kafka;

import com.nixspace.infrastructure.kafka.events.KafkaTopics;
import com.nixspace.infrastructure.kafka.events.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a message lifecycle event (created / updated / deleted).
     * Key = channelId so events for the same channel land on the same partition,
     * preserving ordering.
     */
    @Async
    public void publishMessageCreated(MessageEvent event) {
        String key = String.valueOf(event.getChannelId());
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.MESSAGE_EVENTS, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish MessageEvent [type={}, channelId={}]: {}",
                        event.getType(), event.getChannelId(), ex.getMessage());
            } else {
                log.debug("MessageEvent published [type={}, channelId={}, offset={}]",
                        event.getType(), event.getChannelId(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
