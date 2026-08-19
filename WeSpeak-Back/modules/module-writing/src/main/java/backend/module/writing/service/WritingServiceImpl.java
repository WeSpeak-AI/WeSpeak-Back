package backend.module.writing.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.event.payload.UserStatEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.infra.Snowflake;
import backend.module.writing.domain.Essay;
import backend.module.writing.repository.EssayRepository;
import backend.module.writing.repository.TopicSummaryRepository;
import backend.module.writing.dto.CorrectionResponse;
import backend.module.writing.dto.EssayRequest;
import backend.module.writing.dto.EssayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WritingServiceImpl implements WritingService {

    private final EssayRepository essayRepository;
    private final TopicSummaryRepository topicSummaryRepository;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public String getRandomTopic() {
        return topicSummaryRepository.findRandom()
                .orElseThrow(() -> new BusinessException(ErrorCode.WRITING_TOPIC_NOT_FOUND))
                .getTitle();
    }

    @Override
    @Transactional
    public void deleteMyEssay(String email, Long essayId) {
        Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESSAY_NOT_FOUND));
        if (!essay.getUserEmail().equals(email)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        essayRepository.delete(essay);
    }

    @Override
    @Transactional
    public EssayResponse save(String email, EssayRequest request) {
        Essay essay = Essay.builder()
                .essayId(snowflake.nextId())
                .userEmail(email)
                .topic(request.getTopic())
                .content(request.getContent())
                .type(request.getType())
                .hasCorrected(false)
                .build();
        Essay saved = essayRepository.save(essay);
        outboxEventPublisher.publish(EventType.AI_CORRECTION, AiCorrectionEventPayload.builder()
                .essayId(saved.getEssayId())
                .content(saved.getContent())
                .build());
        outboxEventPublisher.publish(EventType.STUDY_COMPLETED, StudyCompletedEventPayload.builder()
                .email(email)
                .studiedAt(LocalDate.now())
                .build());
        outboxEventPublisher.publish(EventType.ESSAY_SUBMITTED, UserStatEventPayload.builder()
                .email(email)
                .recordedAt(LocalDateTime.now())
                .build());
        return EssayResponse.from(saved);
    }

    @Override
    public List<EssayResponse> getMyEssays(String email) {
        return essayRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(EssayResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CorrectionResponse getMyEssay(Long essayId) {
        log.debug("[getMyEssay] searching essayId={}", essayId);
        Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESSAY_NOT_FOUND));

        return CorrectionResponse.from(essay);
    }


}
