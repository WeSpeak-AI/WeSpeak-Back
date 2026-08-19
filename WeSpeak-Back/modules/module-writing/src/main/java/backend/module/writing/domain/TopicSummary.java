package backend.module.writing.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TopicSummary {

    @Id
    private Long topicId;

    @Column(nullable = false)
    private String title;

    public void updateTitle(String title) {
        this.title = title;
    }
}
