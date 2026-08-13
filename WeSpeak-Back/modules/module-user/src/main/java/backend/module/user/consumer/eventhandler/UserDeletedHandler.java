package backend.module.user.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserDeletedEventPayload;
import backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedHandler implements EventHandler<UserDeletedEventPayload> {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void handle(Event<UserDeletedEventPayload> event) {
        String email = event.getPayload().getEmail();
        userRepository.findByEmail(email).ifPresent(user -> {
            userRepository.delete(user);
            log.info("[UserDeletedHandler] 유저 삭제 완료. email={}", email);
        });
    }

    @Override
    public boolean supports(Event<UserDeletedEventPayload> event) {
        return event.getEventType() == EventType.USER_DELETED;
    }
}
