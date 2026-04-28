package backend.module.conversation.dto;

import backend.core.domain.topic.Topic;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicResponse {
    private Long topicId;
    private String title;
    private String difficulty;
    private String emoji;
    private String color;

    public static TopicResponse from(Topic topic) {
        return TopicResponse.builder()
                .topicId(topic.getTopicId())
                .title(topic.getTitle())
                .difficulty(topic.getDifficulty())
                .emoji(topic.getEmoji())
                .color(topic.getColor())
                .build();
    }
}
