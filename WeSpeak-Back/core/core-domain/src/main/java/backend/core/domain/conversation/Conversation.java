package backend.core.domain.conversation;

import backend.core.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_sessions")
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

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String details;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private int durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;


    public enum Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
