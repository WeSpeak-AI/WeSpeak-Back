package backend.module.user.consumer;

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
public class StudyCompletedEventConsumer {
    private final StreakUpdateService streakUpdateService;

    @KafkaListener(topics = {
        EventType.EventTopic.USER
    })
    public void listen(String message, Acknowledgment ack) {
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if (event != null) {
                streakUpdateService.handleEvent(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[StudyCompletedEventConsumer.listen] message={}", message, e);
            throw e;
        }
    }
}
