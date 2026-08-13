package backend.module.conversation.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserDeletedEventPayload;
import backend.core.domain.conversation.Conversation;
import backend.module.conversation.repository.ConversationMessageRepository;
import backend.module.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedHandler implements EventHandler<UserDeletedEventPayload> {
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    @Override
    @Transactional
    public void handle(Event<UserDeletedEventPayload> event) {
        String email = event.getPayload().getEmail();

        List<Conversation> conversations = conversationRepository.findByUser_Email(email);
        if (!conversations.isEmpty()) {
            conversationMessageRepository.deleteAllByConversationIn(conversations);
        }
        conversationRepository.deleteAll(conversations);

        log.info("[UserDeletedHandler] conversation 데이터 삭제 완료. email={}", email);
    }

    @Override
    public boolean supports(Event<UserDeletedEventPayload> event) {
        return event.getEventType() == EventType.USER_DELETED;
    }
}
