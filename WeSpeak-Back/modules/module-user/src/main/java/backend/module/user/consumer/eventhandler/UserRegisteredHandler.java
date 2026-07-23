package backend.module.user.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserRegisteredEventPayload;
import backend.module.user.domain.User;
import backend.module.user.domain.UserStatsSnapshot;
import backend.module.user.repository.UserRepository;
import backend.module.user.repository.UserStatsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredHandler implements EventHandler<UserRegisteredEventPayload> {
    private final UserRepository userRepository;
    private final UserStatsSnapshotRepository userStatsSnapshotRepository;

    @Override
    @Transactional
    public void handle(Event<UserRegisteredEventPayload> event) {
        UserRegisteredEventPayload payload = event.getPayload();

        if (userRepository.existsById(payload.getUserId())) {
            log.info("[UserRegisteredHandler] userId={} already exists, skipping (redelivery)", payload.getUserId());
            return;
        }

        User user = User.builder()
                .userId(payload.getUserId())
                .email(payload.getEmail())
                .nickname(payload.getNickname())
                .xp(0)
                .streak(0)
                .mediaTicket(0)
                .build();
        userRepository.save(user);

        userStatsSnapshotRepository.save(UserStatsSnapshot.builder()
                .userId(payload.getUserId())
                .build());
    }

    @Override
    public boolean supports(Event<UserRegisteredEventPayload> event) {
        return event.getEventType() == EventType.USER_REGISTERED;
    }
}
