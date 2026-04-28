package backend.module.writing.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.domain.user.User;
import backend.core.domain.writing.Essay;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.TopicRepository;
import backend.core.infra.repository.UserRepository;
import backend.module.writing.dto.CorrectionResponse;
import backend.module.writing.dto.EssayRequest;
import backend.module.writing.dto.EssayResponse;
import backend.module.writing.repository.EssayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WritingServiceImpl implements WritingService {

    private final EssayRepository essayRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public String getRandomTopic() {
        return topicRepository.findRandom()
                .orElseThrow(() -> new BusinessException(ErrorCode.WRITING_TOPIC_NOT_FOUND))
                .getTitle();
    }

    @Override
    @Transactional
    public EssayResponse save(String email, EssayRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Essay essay = Essay.builder()
                .essayId(snowflake.nextId())
                .user(user)
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
        return EssayResponse.from(saved);
    }

    @Override
    public List<EssayResponse> getMyEssays(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return essayRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(EssayResponse::from)
                .toList();
    }

    @Override
    public CorrectionResponse getMyEssay(Long essayId) {
        Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESSAY_NOT_FOUND));

        return CorrectionResponse.from(essay);
    }


}
