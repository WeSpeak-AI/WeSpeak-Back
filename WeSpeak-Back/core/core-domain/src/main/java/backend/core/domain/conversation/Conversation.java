package backend.core.domain.conversation;

import backend.core.domain.topic.Topic;
import backend.core.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_sessions", indexes = @Index(columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Conversation {

    @Id
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public void close() {
        this.status = Status.CLOSED;
        this.endedAt = LocalDateTime.now();
    }

    public enum Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum Status {
        ACTIVE, CLOSED
    }
}
