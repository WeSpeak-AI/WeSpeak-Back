package backend.module.conversation.repository;

import backend.core.domain.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByStatusAndStartedAtBefore(Conversation.Status status, LocalDateTime threshold);

    @Query("""
            SELECT c
            FROM Conversation c
            JOIN FETCH c.user
            WHERE c.conversationId = :id
            """)
    Optional<Conversation> findByIdWithUser(Long conversationId);
}
