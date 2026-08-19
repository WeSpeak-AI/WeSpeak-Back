package backend.module.reading.dto;

import backend.module.reading.domain.Book;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    @NotNull
    private Book.Level level;

    @NotBlank
    private String category;
}
