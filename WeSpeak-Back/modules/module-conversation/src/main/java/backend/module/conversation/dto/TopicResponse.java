package backend.module.conversation.dto;

import backend.core.domain.topic.Topic;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicResponse {
    @JsonSerialize(using = ToStringSerializer.class)
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
