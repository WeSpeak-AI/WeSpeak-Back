package backend.module.voca.dto;

import backend.core.domain.voca.Word;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WordResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;
    private String term;
    private String meaning;
    private String phonetic;
    private String example;

    public static WordResponse from(Word word) {
        return WordResponse.builder()
                .wordId(word.getWordId())
                .term(word.getTerm())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .phonetic(word.getPhonetic())
                .build();
    }
}
