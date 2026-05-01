package backend.module.reading.service;

import backend.core.domain.reading.Book.Level;
import backend.module.reading.dto.ReadingAiResponse;
import backend.module.reading.dto.ReadingBookContent;
import backend.module.reading.dto.ReadingBookPreviewResponse;
import backend.module.reading.dto.ReadingRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReadingService {

    Page<ReadingBookPreviewResponse> getAllBooks(int page, int size);

    Page<ReadingBookPreviewResponse> getBooksByLevel(Level level, int page, int size);

    List<ReadingBookPreviewResponse> getMyBooks(String email);

    ReadingBookContent getPage(String email, Long bookId, Integer pageNumber);

    Long startBook(String email, ReadingRequest readingRequest);

    ReadingAiResponse processUserSummary(String email, Long bookPageId, byte[] audioBytes);

    ReadingAiResponse getFeedback(Long bookPageId, byte[] audioBytes);

    void deleteMyBook(Long bookId, String email);
}
