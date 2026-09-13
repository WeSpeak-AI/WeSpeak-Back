package backend.module.writing.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EssayCorrectionStatusTest {

    private Essay essay(CorrectionStatus status, boolean hasCorrected) {
        return Essay.builder()
                .essayId(1L)
                .userEmail("user@test.com")
                .topic("topic")
                .content("content")
                .type(Essay.Type.ESSAY)
                .hasCorrected(hasCorrected)
                .correctionStatus(status)
                .build();
    }

    @Test
    void legacyRowWithoutStatusIsDerivedFromHasCorrected() {
        assertThat(essay(null, false).getCorrectionStatus()).isEqualTo(CorrectionStatus.PENDING);
        assertThat(essay(null, true).getCorrectionStatus()).isEqualTo(CorrectionStatus.COMPLETED);
    }

    @Test
    void pendingEssayCanBeMarkedFailed() {
        Essay essay = essay(CorrectionStatus.PENDING, false);

        assertThat(essay.markCorrectionFailed()).isTrue();
        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.FAILED);
        assertThat(essay.isCorrectionFailed()).isTrue();
    }

    @Test
    void legacyPendingEssayCanBeMarkedFailed() {
        Essay essay = essay(null, false);

        assertThat(essay.markCorrectionFailed()).isTrue();
        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.FAILED);
    }

    @Test
    void completedOrFailedEssayIsNotMarkedFailedAgain() {
        Essay completed = essay(CorrectionStatus.COMPLETED, true);
        Essay failed = essay(CorrectionStatus.FAILED, false);

        assertThat(completed.markCorrectionFailed()).isFalse();
        assertThat(completed.getCorrectionStatus()).isEqualTo(CorrectionStatus.COMPLETED);
        assertThat(failed.markCorrectionFailed()).isFalse();
    }

    @Test
    void failedEssayGoesBackToPendingOnRetryAndCompletesOnCorrection() {
        Essay essay = essay(CorrectionStatus.FAILED, false);

        essay.retryCorrection();
        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.PENDING);

        essay.applyCorrection("corrected");
        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.COMPLETED);
        assertThat(essay.isHasCorrected()).isTrue();
        assertThat(essay.getCorrectionResult()).isEqualTo("corrected");
    }
}
