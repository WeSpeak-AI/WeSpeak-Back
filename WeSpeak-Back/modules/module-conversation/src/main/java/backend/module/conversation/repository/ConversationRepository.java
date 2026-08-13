package backend.module.conversation.repository;

import backend.core.domain.conversation.Conversation;
import backend.core.domain.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByTopic(Topic topic);

    List<Conversation> findByStatusAndStartedAtBefore(Conversation.Status status, LocalDateTime threshold);

    @Query("""
            SELECT c
            FROM Conversation c
            JOIN FETCH c.user
            WHERE c.conversationId = :id
            """)
    Optional<Conversation> findByIdWithUser(Long conversationId);

    List<Conversation> findByUser_Email(String email);
}
