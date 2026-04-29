package backend.module.voca.dto;

import java.util.List;

public record VocaGenerationResponse(List<DayResult> days) {

    public record DayResult(
            Integer day,
            String dayTopic,
            List<WordResult> words
    ) {}

    public record WordResult(
            String term,
            String meaning,
            String phonetic,
            String example,
            String imageUrl
    ) {}
}
