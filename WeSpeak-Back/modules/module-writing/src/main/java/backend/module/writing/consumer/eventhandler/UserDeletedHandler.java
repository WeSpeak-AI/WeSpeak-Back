package backend.module.writing.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserDeletedEventPayload;
import backend.module.writing.domain.Essay;
import backend.module.writing.repository.EssayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedHandler implements EventHandler<UserDeletedEventPayload> {
    private final EssayRepository essayRepository;

    @Override
    @Transactional
    public void handle(Event<UserDeletedEventPayload> event) {
        String email = event.getPayload().getEmail();

        List<Essay> essays = essayRepository.findByUserEmailOrderByCreatedAtDesc(email);
        essayRepository.deleteAll(essays);

        log.info("[UserDeletedHandler] writing 데이터 삭제 완료. email={}", email);
    }

    @Override
    public boolean supports(Event<UserDeletedEventPayload> event) {
        return event.getEventType() == EventType.USER_DELETED;
    }
}
