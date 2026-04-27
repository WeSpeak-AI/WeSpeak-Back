package backend.cowrite.common.outboxmessagerelay.pub;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.outboxmessagerelay.Outbox;
import backend.cowrite.common.outboxmessagerelay.OutboxEvent;
import backend.cowrite.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final Snowflake outBoxIdSnowflake = new Snowflake();
    private final Snowflake eventIdSnowflake = new Snowflake();
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(EventType eventType, EventPayload eventPayload, Long documentId) {
        Outbox outbox = Outbox.create(
                outBoxIdSnowflake.nextId(),
                eventType,
                Event.createEvent(
                        eventIdSnowflake.nextId(), eventType, eventPayload
                ).toJson(),
                documentId
        );
        OutboxEvent outboxEvent = OutboxEvent.createOutboxEventByOutbox(outbox);
        applicationEventPublisher.publishEvent(outboxEvent);
    }
}
