package backend.module.reading.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookImageRequest {
    @NotBlank
    String imageUrl;
}
