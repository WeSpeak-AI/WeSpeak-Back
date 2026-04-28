package backend.module.reading.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.domain.reading.Book;
import backend.core.domain.reading.Book.Level;
import backend.core.domain.reading.BookPage;
import backend.core.domain.user.User;
import backend.core.domain.userbook.UserBook;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.UserRepository;
import backend.core.webclient.stt.SttService;
import backend.module.reading.dto.ReadingBookContent;
import backend.module.reading.dto.ReadingBookPreviewResponse;
import backend.module.reading.dto.ReadingRequest;
import backend.module.reading.repository.BookPageRepository;
import backend.module.reading.repository.ReadingBookRepository;
import backend.module.reading.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingServiceImpl implements ReadingService {

    private final ReadingBookRepository readingBookRepository;
    private final BookPageRepository bookPageRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;
    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final SttService sttService;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public Page<ReadingBookPreviewResponse> getAllBooks(int page, int size) {
        return readingBookRepository.findAll(PageRequest.of(page, size))
                .map(ReadingBookPreviewResponse::from);
    }

    @Override
    public Page<ReadingBookPreviewResponse> getBooksByLevel(Level level, int page, int size) {
        return readingBookRepository.findByLevel(level, PageRequest.of(page, size))
                .map(ReadingBookPreviewResponse::from);
    }

    @Override
    public List<ReadingBookPreviewResponse> getMyBooks(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<UserBook> userBooks = userBookRepository.findByUser(user);

        return userBooks.stream()
                .map(ReadingBookPreviewResponse::fromUserBook)
                .toList();
    }

    @Override
    @Transactional
    public ReadingBookContent getPage(String email, Long bookId, Integer pageNumber) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Book book = readingBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));

        UserBook userBook = userBookRepository.findByUserAndBook(user, book)
                .orElseGet(() -> userBookRepository.save(UserBook.builder()
                        .userBookId(snowflake.nextId())
                        .user(user)
                        .book(book)
                        .currentPage(1)
                        .build()));

        int targetPage = (pageNumber != null) ? pageNumber : userBook.getCurrentPage();

        BookPage bookPage = bookPageRepository.findByBookAndPageNumber(book, targetPage)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));

        userBook.updateCurrentPage(targetPage);
        outboxEventPublisher.publish(EventType.STUDY_COMPLETED, StudyCompletedEventPayload.builder()
                .email(email)
                .studiedAt(LocalDate.now())
                .build());

        return ReadingBookContent.from(bookPage);
    }

    @Override
    @Transactional
    public String processUserSummary(String email, Long bookPageId, byte[] audioBytes) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.consumeTicket();

        String userSummary = sttService.transcribe(audioBytes);
        return getFeedback(bookPageId, userSummary);
    }

    @Override
    public String getFeedback(Long bookPageId, String userSummary) {
        BookPage bookPage = bookPageRepository.findById(bookPageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));

        return aiWebClient.post()
                .uri("/feedback")
                .bodyValue(Map.of("messages", List.of(
                        Map.of("role", "user", "content", bookPage.getContent()),
                        Map.of("role", "assistant", "content", "Give me a summary."),
                        Map.of("role", "user", "content", userSummary)
                )))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    @Transactional
    public Long startBook(String email, ReadingRequest readingRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Book book = readingBookRepository.findById(readingRequest.getBookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));

        return userBookRepository.findByUserAndBook(user, book)
                .orElseGet(() -> userBookRepository.save(UserBook.builder()
                        .userBookId(snowflake.nextId())
                        .book(book)
                        .user(user)
                        .currentPage(1)
                        .build()))
                .getUserBookId();
    }
}
