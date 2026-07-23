package backend.module.conversation.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.event.payload.UserStatEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.conversation.domain.Conversation;
import backend.module.conversation.domain.Topic;
import backend.core.infra.Snowflake;
import backend.module.conversation.repository.TopicRepository;
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
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        Conversation conversation = Conversation.builder()
                .conversationId(snowflake.nextId())
                .userEmail(email)
                .topic(topic)
                .startedAt(LocalDateTime.now())
                .build();

        Long conversationId = conversationRepository.save(conversation).getConversationId();

        outboxEventPublisher.publish(EventType.CONVERSATION_HELD, UserStatEventPayload.builder()
                .email(email)
                .recordedAt(LocalDateTime.now())
                .build());

        return conversationId;
    }

    @Override
    public Conversation getConversation(String sessionId) {
        return conversationRepository.findById(Long.valueOf(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    @Override
    @Transactional
    public void closeSession(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        conversation.close();
        redisTemplate.delete("conversation:history:" + conversationId);
        outboxEventPublisher.publish(EventType.STUDY_COMPLETED, StudyCompletedEventPayload.builder()
                .email(conversation.getUserEmail())
                .studiedAt(LocalDate.now())
                .build());
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, String email) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        conversationRepository.delete(conversation);
    }
}
