package backend.module.conversation.repository;

import backend.module.conversation.domain.Conversation;
import backend.module.conversation.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByTopic(Topic topic);

    List<Conversation> findByUserEmail(String userEmail);

    List<Conversation> findByStatusAndStartedAtBefore(Conversation.Status status, LocalDateTime threshold);
}
