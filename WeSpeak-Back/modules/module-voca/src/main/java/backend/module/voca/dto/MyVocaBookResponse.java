package backend.module.voca.dto;

import backend.core.domain.uservoca.UserVocaBook;
import backend.core.domain.voca.VocaBook;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyVocaBookResponse {

    private String vocaBookId;
    private String title;
    private String imageUrl;
    private String category;
    private String description;
    private int totalDays;
    private int currentDay;

    public static MyVocaBookResponse from(UserVocaBook userVocaBook) {
        VocaBook book = userVocaBook.getVocaBook();
        return MyVocaBookResponse.builder()
                .vocaBookId(String.valueOf(book.getVocaBookId()))
                .title(book.getTitle())
                .imageUrl(book.getImageUrl())
                .category(book.getCategory().name())
                .description(book.getDescription())
                .totalDays(book.getTotalDays())
                .currentDay(userVocaBook.getCurrentDay())
                .build();
    }
}
