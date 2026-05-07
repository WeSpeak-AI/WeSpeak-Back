package backend.module.reading.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.reading.Book;
import backend.core.domain.reading.BookPage;
import backend.core.infra.Snowflake;
import backend.module.reading.dto.BookPageRequest;
import backend.module.reading.dto.BookRequest;
import backend.module.reading.dto.QuickBookRequest;
import backend.module.reading.repository.BookPageRepository;
import backend.module.reading.repository.ReadingBookRepository;
import backend.module.reading.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    public Long quickCreateBook(QuickBookRequest request) {
        // single \n → space, \n\n+ → \n\n (paragraph break preserved)
        String normalized = request.getContent().trim()
                .replaceAll("(?<!\n)\n(?!\n)", " ")
                .replaceAll("\n{2,}", "\n\n");

        // split into paragraphs, distribute into ~180-word pages keeping paragraph boundaries
        String[] paragraphs = normalized.split("\n\n");
        int wordsPerPage = 200;

        List<String> pageContents = new ArrayList<>();
        List<String> currentParas = new ArrayList<>();
        int currentWordCount = 0;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            int paraWords = trimmed.split("\\s+").length;

            if (currentWordCount + paraWords > wordsPerPage && !currentParas.isEmpty()) {
                pageContents.add(String.join("\n\n", currentParas));
                currentParas = new ArrayList<>();
                currentWordCount = 0;
            }
            currentParas.add(trimmed);
            currentWordCount += paraWords;
        }
        if (!currentParas.isEmpty()) {
            pageContents.add(String.join("\n\n", currentParas));
        }

        List<String> headerPages = new ArrayList<>();
        if (hasContent(request.getTitlePageContent()))   headerPages.add(request.getTitlePageContent());
        if (hasContent(request.getChapterPageContent())) headerPages.add(request.getChapterPageContent());
        if (hasContent(request.getIntroPageContent()))   headerPages.add(request.getIntroPageContent());

        Book book = Book.builder()
                .bookId(snowflake.nextId())
                .title(request.getTitle())
                .level(request.getLevel())
                .category(request.getCategory())
                .totalPages(headerPages.size() + pageContents.size())
                .build();
        readingBookRepository.save(book);

        int pageNum = 1;
        for (String headerContent : headerPages) {
            bookPageRepository.save(BookPage.builder()
                    .bookPageId(snowflake.nextId())
                    .book(book)
                    .pageNumber(pageNum++)
                    .content(headerContent)
                    .build());
        }
        for (String bodyContent : pageContents) {
            bookPageRepository.save(BookPage.builder()
                    .bookPageId(snowflake.nextId())
                    .book(book)
                    .pageNumber(pageNum++)
                    .content(bodyContent)
                    .build());
        }

        return book.getBookId();
    }

    private boolean hasContent(String s) {
        return s != null && !s.isBlank();
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
        if (userBookRepository.existsByBookBookId(bookId)) {
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
