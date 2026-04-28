package backend.module.reading.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.reading.Book;
import backend.core.domain.reading.BookPage;
import backend.core.infra.Snowflake;
import backend.module.reading.dto.BookPageRequest;
import backend.module.reading.dto.BookRequest;
import backend.module.reading.repository.BookPageRepository;
import backend.module.reading.repository.ReadingBookRepository;
import backend.module.reading.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookServiceImpl implements AdminBookService {

    private final ReadingBookRepository readingBookRepository;
    private final BookPageRepository bookPageRepository;
    private final UserBookRepository userBookRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public Long createBook(BookRequest request) {
        Book book = Book.builder()
                .bookId(snowflake.nextId())
                .title(request.getTitle())
                .level(request.getLevel())
                .category(request.getCategory())
                .build();
        return readingBookRepository.save(book).getBookId();
    }

    @Override
    @Transactional
    public void updateBook(Long bookId, BookRequest request) {
        Book book = readingBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
        book.update(request.getTitle(), request.getLevel(), request.getCategory());
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = readingBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
        if (userBookRepository.existsByBook(book)) {
            throw new BusinessException(ErrorCode.READING_BOOK_IN_USE);
        }
        readingBookRepository.delete(book);
    }

    @Override
    @Transactional
    public Long createBookPage(Long bookId, BookPageRequest request) {
        Book book = readingBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
        BookPage bookPage = BookPage.builder()
                .bookPageId(snowflake.nextId())
                .book(book)
                .pageNumber(request.getPageNumber())
                .content(request.getContent())
                .build();
        return bookPageRepository.save(bookPage).getBookPageId();
    }

    @Override
    @Transactional
    public void updateBookPage(Long bookPageId, BookPageRequest request) {
        BookPage bookPage = bookPageRepository.findById(bookPageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
        bookPage.update(request.getPageNumber(), request.getContent());
    }

    @Override
    @Transactional
    public void deleteBookPage(Long bookPageId) {
        BookPage bookPage = bookPageRepository.findById(bookPageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_BOOK_NOT_FOUND));
        bookPageRepository.delete(bookPage);
    }
}
