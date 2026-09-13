package backend.module.writing.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "essays", indexes = {@Index(columnList = "user_email") })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Essay {

    @Id
    private Long essayId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

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

    // 컬럼 추가 이전에 저장된 행은 null — getCorrectionStatus()에서 hasCorrected로 판단한다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CorrectionStatus correctionStatus;

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

    public CorrectionStatus getCorrectionStatus() {
        if (correctionStatus != null) {
            return correctionStatus;
        }
        return hasCorrected ? CorrectionStatus.COMPLETED : CorrectionStatus.PENDING;
    }

    public void applyCorrection(String result) {
        this.correctionResult = result;
        this.hasCorrected = true;
        this.correctionStatus = CorrectionStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * PENDING 상태일 때만 FAILED로 전이한다. 이미 완료됐거나 실패로 기록된 경우 false를 반환한다.
     */
    public boolean markCorrectionFailed() {
        if (getCorrectionStatus() != CorrectionStatus.PENDING) {
            return false;
        }
        this.correctionStatus = CorrectionStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    public boolean isCorrectionFailed() {
        return getCorrectionStatus() == CorrectionStatus.FAILED;
    }

    public void retryCorrection() {
        this.correctionStatus = CorrectionStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Type {
        ESSAY, PARAGRAPH
    }
}
