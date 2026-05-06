package backend.module.reading.dto;

import backend.core.domain.reading.BookPage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadingBookContent {
    private String bookPageId;
    private int pageNumber;
    private int totalPages;
    private String content;

    public static ReadingBookContent from(BookPage bookPage) {
        return ReadingBookContent.builder()
                .bookPageId(String.valueOf(bookPage.getBookPageId()))
                .pageNumber(bookPage.getPageNumber())
                .totalPages(bookPage.getBook().getTotalPages())
                .content(bookPage.getContent())
                .build();
    }
}
