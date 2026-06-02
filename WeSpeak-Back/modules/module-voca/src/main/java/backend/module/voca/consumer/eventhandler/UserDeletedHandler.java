package backend.module.voca.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.UserDeletedEventPayload;
import backend.core.domain.test.Test;
import backend.core.domain.uservoca.UserVocaBook;
import backend.core.infra.repository.CustomVocaBookRepository;
import backend.module.voca.repository.CorrectWordRepository;
import backend.module.voca.repository.IncorrectWordRepository;
import backend.module.voca.repository.TestRepository;
import backend.module.voca.repository.UserVocaBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedHandler implements EventHandler<UserDeletedEventPayload> {
    private final TestRepository testRepository;
    private final CorrectWordRepository correctWordRepository;
    private final IncorrectWordRepository incorrectWordRepository;
    private final UserVocaBookRepository userVocaBookRepository;
    private final CustomVocaBookRepository customVocaBookRepository;

    @Override
    @Transactional
    public void handle(Event<UserDeletedEventPayload> event) {
        String email = event.getPayload().getEmail();

        List<Test> tests = testRepository.findByUser_Email(email);
        for (Test test : tests) {
            correctWordRepository.deleteByTestId(test.getTestId());
            incorrectWordRepository.deleteByTestId(test.getTestId());
        }
        testRepository.deleteAll(tests);

        List<UserVocaBook> userVocaBooks = userVocaBookRepository.findByUserEmail(email);
        userVocaBookRepository.deleteAll(userVocaBooks);

        customVocaBookRepository.deleteAll(customVocaBookRepository.findAllByUserEmail(email));

        log.info("[UserDeletedHandler] voca 데이터 삭제 완료. email={}", email);
    }

    @Override
    public boolean supports(Event<UserDeletedEventPayload> event) {
        return event.getEventType() == EventType.USER_DELETED;
    }
}
