package backend.module.conversation.repository;

import backend.core.domain.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByStatusAndStartedAtBefore(Conversation.Status status, LocalDateTime threshold);
}
