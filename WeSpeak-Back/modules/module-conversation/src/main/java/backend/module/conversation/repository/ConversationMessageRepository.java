package backend.module.conversation.repository;

import backend.module.conversation.domain.Conversation;
import backend.module.conversation.domain.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    @Modifying
    @Query("DELETE FROM ConversationMessage m WHERE m.conversation IN :conversations")
    void deleteAllByConversationIn(@Param("conversations") List<Conversation> conversations);
}
