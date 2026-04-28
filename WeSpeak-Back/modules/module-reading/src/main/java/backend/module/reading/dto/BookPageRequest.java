package backend.module.reading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookPageRequest {

    @NotNull
    private Integer pageNumber;

    @NotBlank
    private String content;
}
