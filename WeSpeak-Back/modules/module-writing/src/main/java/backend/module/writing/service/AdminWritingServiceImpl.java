package backend.module.writing.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.writing.domain.CorrectionStatus;
import backend.module.writing.domain.Essay;
import backend.module.writing.repository.EssayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWritingServiceImpl implements AdminWritingService {

    static final int RETRY_BATCH_SIZE = 100;

    private final EssayRepository essayRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    @Transactional
    public void retryCorrection(Long essayId) {
        Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESSAY_NOT_FOUND));
        if (!essay.isCorrectionFailed()) {
            throw new BusinessException(ErrorCode.ESSAY_CORRECTION_NOT_FAILED);
        }
        requestCorrection(essay);
    }

    // AI 서버 복구 확인 후 호출한다. FAILED 건이 남아 있으면 반환값이 0이 될 때까지 반복 호출한다.
    @Override
    @Transactional
    public int retryFailedCorrections() {
        List<Essay> failedEssays = essayRepository.findByCorrectionStatusOrderByCreatedAtAsc(
                CorrectionStatus.FAILED, Pageable.ofSize(RETRY_BATCH_SIZE));
        failedEssays.forEach(this::requestCorrection);
        log.info("[AdminWritingService.retryFailedCorrections] requested={}", failedEssays.size());
        return failedEssays.size();
    }

    // 상태 변경과 이벤트 발행을 같은 트랜잭션(Outbox)으로 묶는다.
    private void requestCorrection(Essay essay) {
        essay.retryCorrection();
        outboxEventPublisher.publish(EventType.AI_CORRECTION, AiCorrectionEventPayload.builder()
                .essayId(essay.getEssayId())
                .content(essay.getContent())
                .build());
    }
}
