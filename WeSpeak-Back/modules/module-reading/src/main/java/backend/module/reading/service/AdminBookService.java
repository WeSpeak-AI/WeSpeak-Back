package backend.module.reading.service;

import backend.module.reading.dto.BookImageRequest;
import backend.module.reading.dto.BookPageRequest;
import backend.module.reading.dto.BookRequest;
import backend.module.reading.dto.QuickBookRequest;
import org.springframework.web.multipart.MultipartFile;

public interface AdminBookService {
    Long createBook(BookRequest request);
    Long quickCreateBook(QuickBookRequest request);
    void updateBook(Long bookId, BookRequest request);
    void updateBookImage(Long bookId, BookImageRequest request);
    void deleteBook(Long bookId);
    Long createBookPage(Long bookId, BookPageRequest request);
    void updateBookPage(Long bookPageId, BookPageRequest request);
    void deleteBookPage(Long bookPageId);
    String uploadPageImage(Long bookPageId, MultipartFile file);
}
