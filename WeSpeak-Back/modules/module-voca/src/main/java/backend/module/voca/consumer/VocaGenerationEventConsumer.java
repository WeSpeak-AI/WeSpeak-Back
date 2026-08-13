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

    @KafkaListener(topics = {EventType.EventTopic.VOCA}, containerFactory = "vocaGenerationKafkaListenerContainerFactory")
    public void listenVoca(String message, Acknowledgment ack) {
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if (event != null) {
                vocaGenerationService.handleEvent(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[VocaGenerationEventConsumer.listenVoca] message={}", message, e);
            throw e;
        }
    }

    @KafkaListener(topics = {EventType.EventTopic.IMAGE}, containerFactory = "vocaGenerationKafkaListenerContainerFactory")
    public void listenImage(String message, Acknowledgment ack) {
        try {
            Event<EventPayload> event = Event.fromJson(message);
            if(event != null) {
                vocaGenerationService.handleEvent(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[VocaGenerationEventConsumer.listenImage] message ={}", message, e);
            throw e;
        }
    }
}
