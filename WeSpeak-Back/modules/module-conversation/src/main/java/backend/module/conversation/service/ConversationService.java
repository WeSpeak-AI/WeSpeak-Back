package backend.module.conversation.service;

import backend.core.domain.conversation.Conversation;
import backend.module.conversation.dto.ConversationRequest;
import backend.module.conversation.dto.TopicResponse;

import java.util.List;

public interface ConversationService {

    List<TopicResponse> getTopics();

    Long startConversation(String email, ConversationRequest request);

    Conversation getConversation(String sessionId);

    void closeSession(Long conversationId);
}
