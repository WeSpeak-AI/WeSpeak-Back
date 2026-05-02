package backend.core.domain.user;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    private Long userId;

    @Version
    private Long version;

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

    private int mediaTicket;

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

    public void addTicket(int amount) {
        this.mediaTicket += amount;
    }

    public void consumeTicket() {
        if (this.mediaTicket <= 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_TICKET);
        }
        this.mediaTicket--;
    }

    public enum Role {
        BASIC_USER, PREMIUM_USER, ADMIN
    }
}
