package backend.module.reading.dto;

import backend.module.reading.domain.Book;
import backend.module.reading.domain.UserBook;
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
    private String imageUrl;
    private int currentPage;
    private int totalPage;

    public static ReadingBookPreviewResponse from(Book book) {
        return ReadingBookPreviewResponse.builder()
                .bookId(String.valueOf(book.getBookId()))
                .title(book.getTitle())
                .level(book.getLevel().name())
                .category(book.getCategory())
                .imageUrl(book.getImageUrl())
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
                .imageUrl(book.getImageUrl())
                .level(book.getLevel().name())
                .category(book.getCategory())
                .build();
    }
}
