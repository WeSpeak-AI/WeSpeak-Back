package backend.module.voca.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestResultRequest {
    @NotEmpty
    public List<TestedWord> testedWords;
    @NotNull
    public Integer startDay;
    @NotNull
    public Integer endDay;
    public record TestedWord(
            String term,
            boolean correct
    ){}
}
