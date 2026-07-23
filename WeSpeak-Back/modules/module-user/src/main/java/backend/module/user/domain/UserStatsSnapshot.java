package backend.module.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_stats_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserStatsSnapshot {

    @Id
    private Long userId;

    @Builder.Default
    private long vocaBookCount = 0;

    @Builder.Default
    private long essayCount = 0;

    @Builder.Default
    private long userBookCount = 0;

    @Builder.Default
    private long conversationCount = 0;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void increment(Field field) {
        switch (field) {
            case VOCA_BOOK -> this.vocaBookCount++;
            case ESSAY -> this.essayCount++;
            case USER_BOOK -> this.userBookCount++;
            case CONVERSATION -> this.conversationCount++;
        }
    }

    public enum Field {
        VOCA_BOOK, ESSAY, USER_BOOK, CONVERSATION
    }
}
