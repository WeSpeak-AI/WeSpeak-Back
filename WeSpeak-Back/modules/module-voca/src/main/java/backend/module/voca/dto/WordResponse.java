package backend.module.voca.dto;

import backend.core.domain.voca.Word;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WordResponse {

    private String wordId;
    private String term;
    private String meaning;
    private String phonetic;
    private String example;
    private String imageUrl;

    public static WordResponse from(Word word) {
        return WordResponse.builder()
                .wordId(String.valueOf(word.getWordId()))
                .term(word.getTerm())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .phonetic(word.getPhonetic())
                .imageUrl(word.getImageUrl())
                .build();
    }
}
