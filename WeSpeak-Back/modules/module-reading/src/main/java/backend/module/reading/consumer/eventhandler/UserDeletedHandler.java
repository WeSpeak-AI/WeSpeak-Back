package backend.module.reading.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserDeletedEventPayload;
import backend.module.reading.domain.UserBook;
import backend.module.reading.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedHandler implements EventHandler<UserDeletedEventPayload> {
    private final UserBookRepository userBookRepository;

    @Override
    @Transactional
    public void handle(Event<UserDeletedEventPayload> event) {
        String email = event.getPayload().getEmail();

        List<UserBook> userBooks = userBookRepository.findByUserEmail(email);
        userBookRepository.deleteAll(userBooks);

        log.info("[UserDeletedHandler] reading 데이터 삭제 완료. email={}", email);
    }

    @Override
    public boolean supports(Event<UserDeletedEventPayload> event) {
        return event.getEventType() == EventType.USER_DELETED;
    }
}
