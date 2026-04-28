package backend.module.conversation.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.topic.Topic;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.TopicRepository;
import backend.module.conversation.dto.TopicRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTopicServiceImpl implements AdminTopicService {

    private final TopicRepository topicRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public Long createTopic(TopicRequest request) {
        Topic topic = Topic.builder()
                .topicId(snowflake.nextId())
                .title(request.getTitle())
                .difficulty(request.getDifficulty())
                .emoji(request.getEmoji())
                .color(request.getColor())
                .build();
        return topicRepository.save(topic).getTopicId();
    }

    @Override
    @Transactional
    public void updateTopic(Long topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.update(request.getTitle(), request.getDifficulty(), request.getEmoji(), request.getColor());
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topicRepository.delete(topic);
    }
}
