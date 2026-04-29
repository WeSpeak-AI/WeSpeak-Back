package backend.module.voca.dto;

import backend.core.domain.voca.VocaBook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocaBookRequest {

    @NotBlank
    private String title;

    @NotNull
    private VocaBook.Category category;

    @NotBlank
    private String description;

    private int numberOfDays;
}
