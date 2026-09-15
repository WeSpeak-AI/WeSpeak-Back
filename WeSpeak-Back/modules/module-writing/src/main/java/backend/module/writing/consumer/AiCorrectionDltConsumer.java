package backend.module.writing.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;
import backend.core.common.event.EventType;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 재시도를 모두 소진한 AI 교정 이벤트(writing.DLT)를 소비해 에세이를 FAILED로 기록한다.
 * 원래 토픽으로 자동 재발행하지 않는다 — 재처리는 AI 서버 복구 확인 후 운영자 API로 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCorrectionDltConsumer {
    private final AiCorrectionSaveService aiCorrectionSaveService;

    @KafkaListener(topics = {
        EventType.EventTopic.WRITING + ".DLT"
    }, containerFactory = "aiCorrectionDltKafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.warn("[AiCorrectionDltConsumer.listen] reason={}, message={}",
                headerValue(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE), record.value());
        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            if (event == null || !(event.getPayload() instanceof AiCorrectionEventPayload payload)) {
                log.warn("[AiCorrectionDltConsumer.listen] not an AI_CORRECTION event, skipping. message={}", record.value());
                ack.acknowledge();
                return;
            }
            aiCorrectionSaveService.markCorrectionFailed(payload.getEssayId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[AiCorrectionDltConsumer.listen] message={}", record.value(), e);
            throw e;
        }
    }

    private String headerValue(ConsumerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
