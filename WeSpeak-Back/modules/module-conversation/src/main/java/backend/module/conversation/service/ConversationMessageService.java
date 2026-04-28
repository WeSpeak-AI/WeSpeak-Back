package backend.module.conversation.service;

import backend.core.domain.conversation.Conversation;

public interface ConversationMessageService {
    public void saveUserMessageToDB(Conversation conversation, String userMessage);
    public void saveAiMessageToDB(Conversation conversation, String assistantMessage);
}
