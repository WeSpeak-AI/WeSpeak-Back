package backend.module.reading.consumer;

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
public class UserDeletedEventConsumer {
    private final UserDeletedService userDeletedService;

    @KafkaListener(topics = {EventType.EventTopic.USER_DELETED})
    public void listen(String message, Acknowledgment ack) {
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if (event != null) {
                userDeletedService.handleEvent(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[UserDeletedEventConsumer.listen] message={}", message, e);
            throw e;
        }
    }
}
