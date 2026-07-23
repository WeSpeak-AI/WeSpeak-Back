package backend.module.voca.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.event.payload.UserStatEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.infra.Snowflake;
import backend.module.voca.domain.UserVocaBook;
import backend.module.voca.domain.VocaBook;
import backend.module.voca.dto.MyVocaBookResponse;
import backend.module.voca.dto.VocaBookDayResponse;
import backend.module.voca.dto.VocaBookResponse;
import backend.module.voca.dto.WordResponse;
import backend.module.voca.repository.UserVocaBookRepository;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VocaServiceImpl implements VocaService {

    private final VocaBookRepository vocaBookRepository;
    private final VocaBookDayRepository vocaBookDayRepository;
    private final UserVocaBookRepository userVocaBookRepository;
    private final WordRepository wordRepository;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public Page<VocaBookResponse> getAllVocas(int page, int size) {
        return vocaBookRepository.findAll(PageRequest.of(page, size))
                .map(VocaBookResponse::from);
    }

    @Override
    public Page<VocaBookResponse> getVocasByCategory(VocaBook.Category category, int page, int size) {
        return vocaBookRepository.findByCategory(category, PageRequest.of(page, size))
                .map(VocaBookResponse::from);
    }

    @Override
    @Transactional
    public void startVoca(String email, Long bookId) {
        if (userVocaBookRepository.existsByUserEmailAndVocaBookVocaBookId(email, bookId)) {
            throw new BusinessException(ErrorCode.ALREADY_STARTED_VOCA);
        }
        VocaBook vocaBook = vocaBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));
        try {
            userVocaBookRepository.save(UserVocaBook.builder()
                    .userBookId(snowflake.nextId())
                    .userEmail(email)
                    .vocaBook(vocaBook)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_STARTED_VOCA);
        }

        outboxEventPublisher.publish(EventType.VOCA_BOOK_ENROLLED, UserStatEventPayload.builder()
                .email(email)
                .recordedAt(LocalDateTime.now())
                .build());
    }

    @Override
    public List<MyVocaBookResponse> getMyVocas(String email) {
        return userVocaBookRepository.findByUserEmail(email).stream()
                .map(MyVocaBookResponse::from)
                .toList();
    }

    @Override
    public List<VocaBookDayResponse> getAllDaysByBook(Long bookId) {
        return vocaBookDayRepository.findByVocaBook_VocaBookId(bookId).stream()
                .map(VocaBookDayResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<WordResponse> getWordsByDay(String email, Long bookId, int dayNumber) {
        outboxEventPublisher.publish(EventType.STUDY_COMPLETED, StudyCompletedEventPayload.builder()
                .email(email)
                .studiedAt(LocalDate.now())
                .build());

        return wordRepository.findByVocaBookDay_VocaBook_VocaBookIdAndVocaBookDay_Day(bookId, dayNumber).stream()
                .map(WordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMyVoca(Long bookId, String email) {
        UserVocaBook userVocaBook = userVocaBookRepository.findByUserEmailAndVocaBookVocaBookId(email, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_VOCA_BOOK_NOT_FOUND));
        userVocaBookRepository.delete(userVocaBook);
    }
}
