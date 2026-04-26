package com.nixspace.infrastructure.kafka;

import com.nixspace.infrastructure.kafka.events.KafkaTopics;
import com.nixspace.infrastructure.kafka.events.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Consume message events from Kafka and push them to subscribed WebSocket clients.
     * Clients subscribe to /topic/channels/{channelId}
     */
    @KafkaListener(
            topics = KafkaTopics.MESSAGE_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessageEvent(
            MessageEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Consumed MessageEvent [type={}, channelId={}, partition={}, offset={}]",
                    event.getType(), event.getChannelId(), partition, offset);

            String destination = "/topic/channels/" + event.getChannelId();
            messagingTemplate.convertAndSend(destination, event);

            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Error processing MessageEvent [channelId={}, offset={}]: {}",
                    event.getChannelId(), offset, ex.getMessage(), ex);
            // Do NOT acknowledge → triggers retry based on consumer config
        }
    }
}
