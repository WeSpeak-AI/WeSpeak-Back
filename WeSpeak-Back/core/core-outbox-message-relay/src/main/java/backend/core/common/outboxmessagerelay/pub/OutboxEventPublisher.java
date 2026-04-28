package backend.core.common.outboxmessagerelay.pub;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;
import backend.core.common.event.EventType;
import backend.core.common.outboxmessagerelay.Outbox;
import backend.core.common.outboxmessagerelay.OutboxEvent;
import backend.core.infra.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final Snowflake snowflake;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(EventType eventType, EventPayload eventPayload) {
        Outbox outbox = Outbox.create(
                snowflake.nextId(),
                eventType,
                Event.createEvent(
                        snowflake.nextId(), eventType, eventPayload
                ).toJson()
        );
        OutboxEvent outboxEvent = OutboxEvent.createOutboxEventByOutbox(outbox);
        applicationEventPublisher.publishEvent(outboxEvent);
    }
}
