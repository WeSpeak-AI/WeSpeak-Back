package backend.module.voca.dto;

import backend.core.domain.voca.VocaBook;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VocaBookResponse {

    private Long vocaBookId;
    private String title;
    private String category;
    private String description;
    private int totalDays;

    public static VocaBookResponse from(VocaBook book) {
        return VocaBookResponse.builder()
                .vocaBookId(book.getVocaBookId())
                .title(book.getTitle())
                .category(book.getCategory().name())
                .description(book.getDescription())
                .totalDays(book.getTotalDays())
                .build();
    }
}
