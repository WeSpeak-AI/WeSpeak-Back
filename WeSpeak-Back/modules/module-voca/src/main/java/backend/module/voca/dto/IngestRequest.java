package backend.module.voca.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class IngestRequest {
    @NotEmpty
    private List<String> files;
}
