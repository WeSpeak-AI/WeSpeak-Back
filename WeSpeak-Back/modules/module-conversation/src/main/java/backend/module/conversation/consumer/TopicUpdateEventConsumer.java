package backend.module.conversation.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;
import backend.core.common.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicUpdateEventConsumer {
    private final TopicUpdateService topicUpdateService;

    @KafkaListener(topics = {
            EventType.EventTopic.TOPIC
    })
    public void listen(String message, Acknowledgment ack) {
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if (event != null) {
                topicUpdateService.handleEvent(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[TopicUpdateEventConsumer.listen] message={}", message, e);
            throw e;
        }
    }
}
