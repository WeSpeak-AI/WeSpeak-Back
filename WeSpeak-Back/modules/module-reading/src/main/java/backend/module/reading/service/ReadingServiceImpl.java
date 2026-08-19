package backend.module.reading.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.event.payload.UserStatEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.reading.domain.Book;
import backend.module.reading.domain.Book.Level;
import backend.module.reading.domain.BookPage;
import backend.module.reading.domain.UserBook;
import backend.core.infra.Snowflake;
import backend.module.reading.dto.ReadingAiResponse;
import backend.module.reading.dto.ReadingBookContent;
import backend.module.reading.dto.ReadingBookPreviewResponse;
import backend.module.reading.dto.ReadingRequest;
import backend.module.reading.repository.BookPageRepository;
import backend.module.reading.repository.ReadingBookRepository;
import backend.module.reading.repository.UserBookRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingServiceImpl implements ReadingService {

    private final ReadingBookRepository readingBookRepository;
    private final BookPageRepository bookPageRepository;
    private final UserBookRepository userBookRepository;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${ai.server.url}")
    private String aiServerUrl;
    private RestClient restClient;

    @Value("${user.service.url}")
    private String userServiceUrl;
    private RestClient userServiceClient;

    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(50));
        restClient = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .baseUrl(aiServerUrl)
                .build();
        userServiceClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

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
        return userBookRepository.findByUserEmail(email).stream()
                .map(ReadingBookPreviewResponse::fromUserBook)
                .toList();
    }

    @Override
    @Transactional
    public ReadingBookContent getPage(String email, Long bookId, Integer pageNumber) {
        UserBook userBook = userBookRepository.findByUserEmailAndBookBookId(email, bookId)
                .orElseGet(() -> {
                    Book book = readingBookRepository.findById(bookId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
                    UserBook created = userBookRepository.save(UserBook.builder()
                            .userBookId(snowflake.nextId())
                            .userEmail(email)
                            .book(book)
                            .currentPage(1)
                            .build());
                    publishBookProgressed(email);
                    return created;
                });

        int targetPage = (pageNumber != null) ? pageNumber : userBook.getCurrentPage();

        BookPage bookPage = bookPageRepository.findByBookIdAndPageNumber(bookId, targetPage)
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
    public ReadingAiResponse processUserSummary(String email, Long bookPageId, byte[] audioBytes) {
        consumeTicket(email);

        return getFeedback(bookPageId, audioBytes);
    }

    private void consumeTicket(String email) {
        try {
            userServiceClient.post()
                    .uri("/internal/users/tickets/consume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_TICKET);
        }
    }

    @Override
    public ReadingAiResponse getFeedback(Long bookPageId, byte[] audioBytes) {
        BookPage bookPage = bookPageRepository.findById(bookPageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() { return "audio.wav"; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioResource);
        body.add("book_content", bookPage.getContent());

        return restClient.post()
                .uri("/feedback")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ReadingAiResponse.class);
    }

    @Override
    @Transactional
    public void deleteMyBook(Long bookId, String email) {
        UserBook userbook = userBookRepository.findByUserEmailAndBookBookId(email, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_BOOK_NOT_FOUND));
        userBookRepository.delete(userbook);
    }

    @Override
    @Transactional
    public Long startBook(String email, ReadingRequest readingRequest) {
        return userBookRepository.findByUserEmailAndBookBookId(email, readingRequest.getBookId())
                .orElseGet(() -> {
                    Book book = readingBookRepository.findById(readingRequest.getBookId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
                    UserBook created = userBookRepository.save(UserBook.builder()
                            .userBookId(snowflake.nextId())
                            .book(book)
                            .userEmail(email)
                            .currentPage(1)
                            .build());
                    publishBookProgressed(email);
                    return created;
                })
                .getUserBookId();
    }

    private void publishBookProgressed(String email) {
        outboxEventPublisher.publish(EventType.USER_BOOK_PROGRESSED, UserStatEventPayload.builder()
                .email(email)
                .recordedAt(LocalDateTime.now())
                .build());
    }
}
