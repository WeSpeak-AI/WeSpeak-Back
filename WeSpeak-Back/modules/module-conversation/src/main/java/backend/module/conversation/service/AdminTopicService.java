package backend.module.conversation.service;

import backend.module.conversation.dto.TopicRequest;

public interface AdminTopicService {
    Long createTopic(TopicRequest request);
    void updateTopic(Long topicId, TopicRequest request);
    void deleteTopic(Long topicId);
}
