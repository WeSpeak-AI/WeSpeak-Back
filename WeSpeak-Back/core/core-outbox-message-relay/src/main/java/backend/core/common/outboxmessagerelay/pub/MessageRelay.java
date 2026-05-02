package backend.core.common.outboxmessagerelay.pub;

import backend.core.common.outboxmessagerelay.Outbox;
import backend.core.common.outboxmessagerelay.OutboxEvent;
import backend.core.common.outboxmessagerelay.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> messageRelayKafkaTemplate;

    @Value("${spring.application.name}")
    private String serviceName;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent = {}", outboxEvent);
        outboxRepository.save(outboxEvent.getOutbox());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("messageRelayPublishEventExecutor")
    public void publishEvent(OutboxEvent outboxEvent) {
        publishEvent(outboxEvent.getOutbox());
    }

    private void publishEvent(Outbox outbox) {
        try {
            messageRelayKafkaTemplate.send(
                    outbox.getEventType().getTopic(),
                    outbox.getPayload()
            ).get(1, TimeUnit.SECONDS);
            outboxRepository.delete(outbox);
        } catch (Exception e) {
            log.error("[MessageRelay.publishEvent] outbox={}", outbox, e);
        }
    }

    @SchedulerLock(
            name = "${spring.application.name}.publishPendingEvent",
            lockAtMostFor = "9s",
            lockAtLeastFor = "5s"
    )
    @Scheduled(
            fixedDelay = 10,
            initialDelay = 5,
            timeUnit = TimeUnit.SECONDS,
            scheduler = "messageRelayPublishPendingEventExecutor"
    )
    public void publishPendingEvent() {
        List<Outbox> outboxes = outboxRepository.findAllByServiceNameAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                serviceName,
                LocalDateTime.now().minusSeconds(10),
                Pageable.ofSize(100)
        );
        for (Outbox outbox : outboxes) {
            publishEvent(outbox);
        }
    }
}
