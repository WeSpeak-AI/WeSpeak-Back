package backend.core.common.event.payload;

import backend.core.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopicUpdateEventPayload implements EventPayload {
    private Long topicId;
    private String title;
    private String difficulty;
    private String content;
}
