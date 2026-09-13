package backend.module.writing.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.writing.domain.CorrectionStatus;
import backend.module.writing.domain.Essay;
import backend.module.writing.repository.EssayRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminWritingServiceImplTest {

    private final EssayRepository essayRepository = mock(EssayRepository.class);
    private final OutboxEventPublisher outboxEventPublisher = mock(OutboxEventPublisher.class);
    private final AdminWritingService service = new AdminWritingServiceImpl(essayRepository, outboxEventPublisher);

    private Essay essay(Long essayId, CorrectionStatus status) {
        return Essay.builder()
                .essayId(essayId).userEmail("user@test.com").topic("topic").content("content-" + essayId)
                .type(Essay.Type.ESSAY).correctionStatus(status)
                .build();
    }

    @Test
    void retryingFailedEssayResetsToPendingAndPublishesAiCorrection() {
        Essay essay = essay(1L, CorrectionStatus.FAILED);
        when(essayRepository.findById(1L)).thenReturn(Optional.of(essay));

        service.retryCorrection(1L);

        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.PENDING);
        ArgumentCaptor<AiCorrectionEventPayload> payload = ArgumentCaptor.forClass(AiCorrectionEventPayload.class);
        verify(outboxEventPublisher).publish(eq(EventType.AI_CORRECTION), payload.capture());
        assertThat(payload.getValue().getEssayId()).isEqualTo(1L);
        assertThat(payload.getValue().getContent()).isEqualTo("content-1");
    }

    @Test
    void retryingNonFailedEssayIsRejected() {
        when(essayRepository.findById(1L)).thenReturn(Optional.of(essay(1L, CorrectionStatus.PENDING)));

        assertThatThrownBy(() -> service.retryCorrection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ESSAY_CORRECTION_NOT_FAILED);
        verifyNoInteractions(outboxEventPublisher);
    }

    @Test
    void retryingMissingEssayIsRejected() {
        when(essayRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retryCorrection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ESSAY_NOT_FOUND);
    }

    @Test
    void bulkRetryRequestsEveryFailedEssayInBatch() {
        List<Essay> failed = List.of(essay(1L, CorrectionStatus.FAILED), essay(2L, CorrectionStatus.FAILED));
        when(essayRepository.findByCorrectionStatusOrderByCreatedAtAsc(eq(CorrectionStatus.FAILED), any(Pageable.class)))
                .thenReturn(failed);

        int requested = service.retryFailedCorrections();

        assertThat(requested).isEqualTo(2);
        assertThat(failed).allMatch(e -> e.getCorrectionStatus() == CorrectionStatus.PENDING);
        verify(outboxEventPublisher, times(2)).publish(eq(EventType.AI_CORRECTION), any());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(essayRepository).findByCorrectionStatusOrderByCreatedAtAsc(eq(CorrectionStatus.FAILED), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(AdminWritingServiceImpl.RETRY_BATCH_SIZE);
    }
}
