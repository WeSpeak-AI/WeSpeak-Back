package backend.module.conversation.consumer;

public interface TopicSaveService {
    void applyContent(Long topicId, String content);
}
