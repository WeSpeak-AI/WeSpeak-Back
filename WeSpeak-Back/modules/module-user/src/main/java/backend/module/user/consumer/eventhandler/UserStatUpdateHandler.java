package backend.module.user.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserStatEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.module.user.domain.User;
import backend.module.user.domain.UserStatsSnapshot;
import backend.module.user.repository.UserRepository;
import backend.module.user.repository.UserStatsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatUpdateHandler implements EventHandler<UserStatEventPayload> {

    private static final Set<EventType> SUPPORTED_TYPES = EnumSet.of(
            EventType.VOCA_BOOK_ENROLLED,
            EventType.ESSAY_SUBMITTED,
            EventType.USER_BOOK_PROGRESSED,
            EventType.CONVERSATION_HELD
    );

    private final UserRepository userRepository;
    private final UserStatsSnapshotRepository userStatsSnapshotRepository;

    @Override
    @Transactional
    public void handle(Event<UserStatEventPayload> event) {
        String email = event.getPayload().getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserStatsSnapshot snapshot = userStatsSnapshotRepository.findByUserId(user.getUserId())
                .orElseGet(() -> UserStatsSnapshot.builder().userId(user.getUserId()).build());

        snapshot.increment(toField(event.getEventType()));
        userStatsSnapshotRepository.save(snapshot);
    }

    @Override
    public boolean supports(Event<UserStatEventPayload> event) {
        return SUPPORTED_TYPES.contains(event.getEventType());
    }

    private UserStatsSnapshot.Field toField(EventType eventType) {
        return switch (eventType) {
            case VOCA_BOOK_ENROLLED -> UserStatsSnapshot.Field.VOCA_BOOK;
            case ESSAY_SUBMITTED -> UserStatsSnapshot.Field.ESSAY;
            case USER_BOOK_PROGRESSED -> UserStatsSnapshot.Field.USER_BOOK;
            case CONVERSATION_HELD -> UserStatsSnapshot.Field.CONVERSATION;
            default -> throw new IllegalArgumentException("Unsupported stat event type: " + eventType);
        };
    }
}
