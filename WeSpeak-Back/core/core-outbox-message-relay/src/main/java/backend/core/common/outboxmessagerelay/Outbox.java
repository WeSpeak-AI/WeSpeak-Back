package backend.core.common.outboxmessagerelay;

import backend.core.common.event.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Table(name = "outbox")
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {

    @Id
    private Long outboxId;

    @Enumerated(EnumType.STRING)
    private EventType eventType;
    private String payload; //eventPayload
    private LocalDateTime createdAt;

    public static Outbox create(Long outboxId, EventType eventType, String payload) {
        Outbox outbox = new Outbox();
        outbox.outboxId = outboxId;
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }
}
