package backend.module.conversation.service;

import backend.module.conversation.domain.Conversation;

public interface ConversationMessageService {
    public void saveUserMessageToDB(Conversation conversation, String userMessage);
    public void saveAiMessageToDB(Conversation conversation, String assistantMessage);
}
