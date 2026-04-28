package backend.module.voca.dto;

import backend.core.domain.voca.Word;
import lombok.Getter;

@Getter
public class TestingWordResponse {
    private String term;
    private String meaning;

    public static TestingWordResponse from(Word word) {
        TestingWordResponse testingWordResponse = new TestingWordResponse();
        testingWordResponse.term = word.getTerm();
        testingWordResponse.meaning = word.getMeaning();
        return testingWordResponse;
    }
}
