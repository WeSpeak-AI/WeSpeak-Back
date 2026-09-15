package backend.module.writing.consumer;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.module.writing.domain.Essay;
import backend.module.writing.repository.EssayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiCorrectionSaveServiceImpl implements AiCorrectionSaveService{

    private final EssayRepository essayRepository;

    @Override
    @Transactional
    public void applyAiCorrection(Long essayId, String correctionResult) {
        Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESSAY_NOT_FOUND));
        essay.applyCorrection(correctionResult);
    }

    // DLT 컨슈머에서 호출된다. 여기서 예외가 나면 같은 레코드가 다시 소비되므로,
    // 에세이가 없거나(삭제됨) 이미 다른 상태인 경우는 로그만 남기고 정상 종료한다.
    @Override
    @Transactional
    public void markCorrectionFailed(Long essayId) {
        essayRepository.findById(essayId).ifPresentOrElse(
                essay -> {
                    if (!essay.markCorrectionFailed()) {
                        log.info("[AiCorrectionSaveService.markCorrectionFailed] skipped: essayId={}, status={}",
                                essayId, essay.getCorrectionStatus());
                    }
                },
                () -> log.warn("[AiCorrectionSaveService.markCorrectionFailed] essay not found: essayId={}", essayId)
        );
    }
}
