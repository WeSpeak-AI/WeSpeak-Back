package backend.module.voca.dto;

import backend.core.domain.customvoca.CustomVocaBook;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

public record CustomVocaBookResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long customVocaBookId,
        int wordCount,
        LocalDateTime createdAt,
        List<CustomWordResponse> words
) {
    public static CustomVocaBookResponse from(CustomVocaBook book) {
        return new CustomVocaBookResponse(
                book.getCustomVocaBookId(),
                book.getWords().size(),
                book.getCreatedAt(),
                book.getWords().stream().map(CustomWordResponse::from).toList()
        );
    }
}
