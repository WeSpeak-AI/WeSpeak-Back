package backend.module.writing.consumer;

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
public class AiCorrectionEventConsumer {
    private final AiCorrectionService aiCorrectionService;

    @KafkaListener(topics = {
        EventType.EventTopic.WRITING
    })
    public void listen(String message, Acknowledgment ack) {
        log.info("[AiCorrectionEventConsumer.listen] received message={}", message);
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if (event == null) {
                log.warn("[AiCorrectionEventConsumer.listen] Event.fromJson returned null. message={}", message);
                ack.acknowledge();
                return;
            }
            aiCorrectionService.handleEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[AiCorrectionEventConsumer.listen] message={}", message, e);
            throw e;
        }
    }

}
