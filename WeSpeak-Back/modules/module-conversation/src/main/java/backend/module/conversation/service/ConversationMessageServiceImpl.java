package backend.module.conversation.service;

import backend.module.conversation.domain.Conversation;
import backend.module.conversation.domain.ConversationMessage;
import backend.core.infra.Snowflake;
import backend.module.conversation.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {

    private final Snowflake snowflake;
    private final ConversationMessageRepository conversationMessageRepository;

    @Override
    @Transactional
    public void saveUserMessageToDB(Conversation conversation, String userMessage) {
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .messageId(snowflake.nextId())
                .conversation(conversation)
                .role(ConversationMessage.Role.USER)
                .content(userMessage)
                .createdAt(LocalDateTime.now())
                .build();

        conversationMessageRepository.save(conversationMessage);
    }

    @Override
    @Transactional
    public void saveAiMessageToDB(Conversation conversation, String assistantMessage) {
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .messageId(snowflake.nextId())
                .conversation(conversation)
                .role(ConversationMessage.Role.ASSISTANT)
                .content(assistantMessage)
                .createdAt(LocalDateTime.now())
                .build();

        conversationMessageRepository.save(conversationMessage);
    }
}
