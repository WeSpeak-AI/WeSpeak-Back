package backend.core.domain.topic;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    private Long topicId;

    private String title;

    private String difficulty;

    private String content;

    private String emoji;

    private String color;

    public void update(String title, String difficulty, String content, String emoji, String color) {
        this.title = title;
        this.difficulty = difficulty;
        this.content = content;
        this.emoji = emoji;
        this.color = color;
    }

    public void applyContent(String aiedContent) {
        this.content = aiedContent;
    }
}
