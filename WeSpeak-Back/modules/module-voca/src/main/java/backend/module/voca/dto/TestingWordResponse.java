package backend.module.voca.dto;

import backend.module.voca.domain.Word;
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
