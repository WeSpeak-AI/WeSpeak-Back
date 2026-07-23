package backend.module.voca.dto;

import backend.module.voca.domain.CustomWord;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CustomWordResponse(
        String customWordId,
        String term,
        String meaning,
        String phonetic,
        String example,
        String imageUrl
) {
    public static CustomWordResponse from(CustomWord word) {
        return new CustomWordResponse(
                String.valueOf(word.getCustomWordId()),
                word.getTerm(),
                word.getMeaning(),
                word.getPhonetic(),
                word.getExample(),
                word.getImageUrl()
        );
    }
}
