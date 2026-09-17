package backend.core.common.outboxmessagerelay.pub;

import backend.core.common.event.EventType;
import backend.core.common.outboxmessagerelay.Outbox;
import backend.core.common.outboxmessagerelay.OutboxEvent;
import backend.core.common.outboxmessagerelay.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MessageRelayTest {

    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final MessageRelay messageRelay = new MessageRelay(outboxRepository, kafkaTemplate);

    @Test
    void publishesWithOutboxIdAsKeySoMessagesSpreadAcrossPartitions() {
        Outbox outbox = Outbox.create(12345L, EventType.AI_CORRECTION, "{\"eventId\":1}", "writing-service");
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        messageRelay.publishEvent(OutboxEvent.createOutboxEventByOutbox(outbox));

        verify(kafkaTemplate).send(eq(EventType.AI_CORRECTION.getTopic()), eq("12345"), eq("{\"eventId\":1}"));
        verify(outboxRepository).delete(outbox);
    }
}
