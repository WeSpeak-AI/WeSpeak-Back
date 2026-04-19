package backend.core.domain.user;

import backend.core.domain.uservoca.UserVocaBook;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 학습 통계
    private int xp;
    private int streak;
    private LocalDate lastStudiedAt;

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

    public void addXp(int amount) {
        this.xp += amount;
    }

    public void updateStreak(LocalDate today) {
        if (lastStudiedAt != null && lastStudiedAt.plusDays(1).equals(today)) {
            this.streak++;
        } else if (lastStudiedAt == null || !lastStudiedAt.equals(today)) {
            this.streak = 1;
        }
        this.lastStudiedAt = today;
    }

    public enum Role {
        BASIC_USER, PREMIUM_USER, ADMIN
    }
}
