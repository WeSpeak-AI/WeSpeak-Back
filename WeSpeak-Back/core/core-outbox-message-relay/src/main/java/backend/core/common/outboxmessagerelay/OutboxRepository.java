package backend.core.common.outboxmessagerelay;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findAllByServiceNameAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            String serviceName,
            LocalDateTime from,
            Pageable pageable
    );
}
