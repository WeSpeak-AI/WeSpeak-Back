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

    private String emoji;

    private String color;

    public void update(String title, String difficulty, String emoji, String color) {
        this.title = title;
        this.difficulty = difficulty;
        this.emoji = emoji;
        this.color = color;
    }
}
