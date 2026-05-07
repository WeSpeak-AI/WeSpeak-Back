package backend.module.user.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.user.User;
import backend.core.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakUpdateHandler implements EventHandler<StudyCompletedEventPayload> {
    private final UserRepository userRepository;
    private static final int XP_PER_STREAK = 5;

    @Override
    @Transactional
    public void handle(Event<StudyCompletedEventPayload> event) {
        User user = userRepository.findByEmail(event.getPayload().getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateStreak(event.getPayload().getStudiedAt());
        user.addXp(XP_PER_STREAK);
    }

    @Override
    public boolean supports(Event<StudyCompletedEventPayload> event) {
        return event.getEventType() == backend.core.common.event.EventType.STUDY_COMPLETED;
    }
}
