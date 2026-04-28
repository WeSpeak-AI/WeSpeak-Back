package backend.module.voca.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WordRequest {

    @NotBlank
    private String term;

    @NotBlank
    private String meaning;

    @NotBlank
    private String phonetic;

    @NotBlank
    private String example;

    private String imageUrl;
}
