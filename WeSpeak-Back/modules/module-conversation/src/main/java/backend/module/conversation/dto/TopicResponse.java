package backend.module.conversation.dto;

import backend.module.conversation.domain.Topic;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicResponse {
    private String topicId;
    private String title;
    private String difficulty;
    private String emoji;
    private String color;

    public static TopicResponse from(Topic topic) {
        return TopicResponse.builder()
                .topicId(String.valueOf(topic.getTopicId()))
                .title(topic.getTitle())
                .difficulty(topic.getDifficulty())
                .emoji(topic.getEmoji())
                .color(topic.getColor())
                .build();
    }
}
