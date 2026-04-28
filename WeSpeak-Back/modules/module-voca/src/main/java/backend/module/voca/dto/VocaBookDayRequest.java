package backend.module.voca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocaBookDayRequest {

    @NotNull
    private Integer day;

    @NotBlank
    private String dayTopic;
}
