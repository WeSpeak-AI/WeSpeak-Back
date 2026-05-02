package backend.module.writing.consumer;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.writing.Essay;
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
}
