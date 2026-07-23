package backend.module.conversation.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.TopicUpdateEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.conversation.domain.Conversation;
import backend.module.conversation.domain.Topic;
import backend.core.infra.Snowflake;
import backend.module.conversation.repository.TopicRepository;
import backend.module.conversation.dto.TopicRequest;
import backend.module.conversation.repository.ConversationMessageRepository;
import backend.module.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTopicServiceImpl implements AdminTopicService {

    private final TopicRepository topicRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;


    @Override
    @Transactional
    public Long createTopic(TopicRequest request) {
        Topic topic = Topic.builder()
                .topicId(snowflake.nextId())
                .title(request.getTitle())
                .difficulty(request.getDifficulty())
                .content(request.getContent())
                .emoji(request.getEmoji())
                .color(request.getColor())
                .build();

        Topic saved = topicRepository.save(topic);
        outboxEventPublisher.publish(EventType.TOPIC_UPDATE, TopicUpdateEventPayload.builder()
                        .topicId(saved.getTopicId())
                        .title(saved.getTitle())
                        .difficulty(saved.getDifficulty())
                        .content(saved.getContent())
                        .build());
        return saved.getTopicId();
    }

    @Override
    @Transactional
    public void updateTopic(Long topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.update(request.getTitle(), request.getDifficulty(), request.getContent(), request.getEmoji(), request.getColor());
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        List<Conversation> conversations = conversationRepository.findByTopic(topic);
        if (!conversations.isEmpty()) {
            conversationMessageRepository.deleteAllByConversationIn(conversations);
            conversationRepository.deleteAll(conversations);
        }
        topicRepository.delete(topic);
    }
}
