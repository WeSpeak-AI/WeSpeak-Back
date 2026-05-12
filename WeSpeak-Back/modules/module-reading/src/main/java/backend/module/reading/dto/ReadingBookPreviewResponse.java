package backend.module.reading.dto;

import backend.core.domain.reading.Book;
import backend.core.domain.userbook.UserBook;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadingBookPreviewResponse {

    private String bookId;
    private String title;
    private String level;
    private String category;
    private int currentPage;
    private int totalPage;

    public static ReadingBookPreviewResponse from(Book book) {
        return ReadingBookPreviewResponse.builder()
                .bookId(String.valueOf(book.getBookId()))
                .title(book.getTitle())
                .level(book.getLevel().name())
                .category(book.getCategory())
                .totalPage(book.getTotalPages())
                .build();
    }

    public static ReadingBookPreviewResponse fromUserBook(UserBook userBook) {
        Book book = userBook.getBook();
        return ReadingBookPreviewResponse.builder()
                .currentPage(userBook.getCurrentPage())
                .totalPage(book.getTotalPages())
                .bookId(String.valueOf(book.getBookId()))
                .title(book.getTitle())
                .level(book.getLevel().name())
                .category(book.getCategory())
                .build();
    }
}
