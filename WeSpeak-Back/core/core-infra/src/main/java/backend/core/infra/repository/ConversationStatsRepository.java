package backend.core.infra.repository;

import backend.core.domain.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationStatsRepository extends JpaRepository<Conversation, Long> {
    long countByUserEmail(String email);
}
