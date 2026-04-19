package backend.core.domain.writing;

import backend.core.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "essays")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Essay {

    @Id
    private Long essayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private Type type;

    // AI 교정 결과
    @Column(columnDefinition = "TEXT")
    private String correctionResult;

    private boolean hasCorrected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void applyCorrection(String result) {
        this.correctionResult = result;
        this.hasCorrected = true;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Type {
        ESSAY, PARAGRAPH
    }
}
