package backend.module.voca.dto;

import backend.core.domain.customvoca.CustomVocaBook;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

public record CustomVocaBookResponse(
        String customVocaBookId,
        String name,
        int wordCount,
        LocalDateTime createdAt,
        List<CustomWordResponse> words
) {
    public static CustomVocaBookResponse from(CustomVocaBook book) {
        return new CustomVocaBookResponse(
                String.valueOf(book.getCustomVocaBookId()),
                book.getName(),
                book.getWords().size(),
                book.getCreatedAt(),
                book.getWords().stream().map(CustomWordResponse::from).toList()
        );
    }
}
