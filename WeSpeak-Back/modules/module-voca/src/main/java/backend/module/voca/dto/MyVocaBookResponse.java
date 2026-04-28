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

    @JsonSerialize(using = ToStringSerializer.class)
    private Long vocaBookId;
    private String title;
    private String category;
    private String description;
    private int totalDays;
    private int currentDay;

    public static MyVocaBookResponse from(UserVocaBook userVocaBook) {
        VocaBook book = userVocaBook.getVocaBook();
        return MyVocaBookResponse.builder()
                .vocaBookId(book.getVocaBookId())
                .title(book.getTitle())
                .category(book.getCategory().name())
                .description(book.getDescription())
                .totalDays(book.getTotalDays())
                .currentDay(userVocaBook.getCurrentDay())
                .build();
    }
}
