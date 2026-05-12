package backend.module.voca.dto;

import backend.core.domain.voca.VocaBook;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VocaBookResponse {

    private String vocaBookId;
    private String title;
    private String imageUrl;
    private String category;
    private String description;
    private int totalDays;

    public static VocaBookResponse from(VocaBook book) {
        return VocaBookResponse.builder()
                .vocaBookId(String.valueOf(book.getVocaBookId()))
                .title(book.getTitle())
                .imageUrl(book.getImageUrl())
                .category(book.getCategory().name())
                .description(book.getDescription())
                .totalDays(book.getTotalDays())
                .build();
    }
}
