package backend.module.voca.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserRegisteredEventPayload;
import backend.core.infra.Snowflake;
import backend.module.voca.domain.CustomVocaBook;
import backend.module.voca.repository.CustomVocaBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredHandler implements EventHandler<UserRegisteredEventPayload> {
    private final CustomVocaBookRepository customVocaBookRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public void handle(Event<UserRegisteredEventPayload> event) {
        String email = event.getPayload().getEmail();

        if (customVocaBookRepository.findByUserEmail(email).isPresent()) {
            log.info("[UserRegisteredHandler] customVocaBook already exists for email={}, skipping (redelivery)", email);
            return;
        }

        customVocaBookRepository.save(CustomVocaBook.builder()
                .customVocaBookId(snowflake.nextId())
                .userEmail(email)
                .build());
    }

    @Override
    public boolean supports(Event<UserRegisteredEventPayload> event) {
        return event.getEventType() == EventType.USER_REGISTERED;
    }
}
