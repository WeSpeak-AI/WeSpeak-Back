package backend.module.conversation.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.TopicUpdateEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.domain.topic.Topic;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.TopicRepository;
import backend.module.conversation.dto.TopicRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTopicServiceImpl implements AdminTopicService {

    private final TopicRepository topicRepository;
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
        topicRepository.delete(topic);
    }
}
