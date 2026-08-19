package backend.module.voca.dto;

import backend.module.voca.domain.IncorrectWord;
import backend.module.voca.domain.Word;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncorrectWordResponse {
    String term;
    String meaning;
    String phonetic;
    String exampleSentence;

    public static IncorrectWordResponse from(IncorrectWord incorrectWord) {
        Word word = incorrectWord.getWord();
        IncorrectWordResponse incorrectWordResponse = new IncorrectWordResponse();
        incorrectWordResponse.term = word.getTerm();
        incorrectWordResponse.meaning = word.getMeaning();
        incorrectWordResponse.phonetic = word.getPhonetic();
        incorrectWordResponse.exampleSentence = word.getExample();
        return incorrectWordResponse;
    }
}
