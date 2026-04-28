package backend.module.voca.dto;

import backend.core.domain.customvoca.CustomWord;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CustomWordResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long customWordId,
        String term,
        String meaning,
        String phonetic,
        String example,
        String imageUrl
) {
    public static CustomWordResponse from(CustomWord word) {
        return new CustomWordResponse(
                word.getCustomWordId(),
                word.getTerm(),
                word.getMeaning(),
                word.getPhonetic(),
                word.getExample(),
                word.getImageUrl()
        );
    }
}
