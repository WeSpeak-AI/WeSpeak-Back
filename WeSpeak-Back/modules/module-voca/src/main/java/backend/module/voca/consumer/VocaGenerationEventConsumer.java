package backend.module.voca.consumer;

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
public class VocaGenerationEventConsumer {

    private final VocaGenerationService vocaGenerationService;

    @KafkaListener(topics = {EventType.EventTopic.VOCA})
    public void listen(String message, Acknowledgment ack) {
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if (event != null) {
                vocaGenerationService.handleEvent(event);
            }
        } catch (Exception e) {
            log.error("[VocaGenerationEventConsumer.listen] message={}", message, e);
        } finally {
            ack.acknowledge();
        }
    }
}
