package backend.module.conversation.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.domain.conversation.Conversation;
import backend.core.domain.topic.Topic;
import backend.core.domain.user.User;
import backend.core.domain.userbook.UserBook;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.TopicRepository;
import backend.core.infra.repository.UserRepository;
import backend.module.conversation.dto.ConversationRequest;
import backend.module.conversation.dto.TopicResponse;
import backend.module.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public List<TopicResponse> getTopics() {
        return topicRepository.findAll().stream()
                .map(TopicResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public Long startConversation(String email, ConversationRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        Conversation conversation = Conversation.builder()
                .conversationId(snowflake.nextId())
                .user(user)
                .topic(topic)
                .startedAt(LocalDateTime.now())
                .build();

        return conversationRepository.save(conversation).getConversationId();
    }

    @Override
    public Conversation getConversation(String sessionId) {
        return conversationRepository.findById(Long.valueOf(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    @Override
    @Transactional
    public void closeSession(Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithUser(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        conversation.close();
        redisTemplate.delete("conversation:history:" + conversationId);
        outboxEventPublisher.publish(EventType.STUDY_COMPLETED, StudyCompletedEventPayload.builder()
                .email(conversation.getUser().getEmail())
                .studiedAt(LocalDate.now())
                .build());
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, String email) {
        Conversation conversation = conversationRepository.findByIdWithUser(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        conversationRepository.delete(conversation);
    }
}
