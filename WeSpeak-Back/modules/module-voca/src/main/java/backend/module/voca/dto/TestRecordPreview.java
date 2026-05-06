package backend.module.voca.dto;

import backend.core.domain.test.Test;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TestRecordPreview {
    String testId;
    LocalDateTime testedAt;
    int startDay;
    int endDay;
    int score;
    int totalWords;

    public static TestRecordPreview from(Test test) {
        TestRecordPreview testRecordPreview = new TestRecordPreview();
        testRecordPreview.testId = String.valueOf(test.getTestId());
        testRecordPreview.testedAt = test.getTestedAt();
        testRecordPreview.startDay = test.getStartDay();
        testRecordPreview.endDay = test.getEndDay();
        testRecordPreview.score = test.getScore();
        testRecordPreview.totalWords = test.getTotalWords();
        return testRecordPreview;
    }
}
