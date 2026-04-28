package backend.module.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String difficulty;

    @NotBlank
    private String emoji;

    @NotBlank
    private String color;
}
