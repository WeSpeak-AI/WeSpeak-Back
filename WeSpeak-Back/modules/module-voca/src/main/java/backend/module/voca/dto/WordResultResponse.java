package backend.module.voca.dto;

import backend.core.domain.test.CorrectWord;
import backend.core.domain.test.IncorrectWord;
import backend.core.domain.voca.Word;
import lombok.Getter;

@Getter
public class WordResultResponse {
    private String term;
    private String meaning;
    private String phonetic;
    private String exampleSentence;
    private boolean correct;

    public static WordResultResponse fromCorrect(CorrectWord correctWord) {
        return from(correctWord.getWord(), true);
    }

    public static WordResultResponse fromIncorrect(IncorrectWord incorrectWord) {
        return from(incorrectWord.getWord(), false);
    }

    private static WordResultResponse from(Word word, boolean correct) {
        WordResultResponse r = new WordResultResponse();
        r.term = word.getTerm();
        r.meaning = word.getMeaning();
        r.phonetic = word.getPhonetic();
        r.exampleSentence = word.getExample();
        r.correct = correct;
        return r;
    }
}
