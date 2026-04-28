package backend.module.writing.dto;

import backend.core.domain.writing.Essay;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EssayRequest {

    @NotBlank
    private String topic;

    @NotBlank
    private String content;

    @NotNull
    private Essay.Type type;
}
