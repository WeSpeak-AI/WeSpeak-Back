package backend.module.reading.dto;

import backend.core.domain.reading.Book;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookRequest {

    @NotBlank
    private String title;

    @NotNull
    private Book.Level level;

    @NotBlank
    private String category;
}
