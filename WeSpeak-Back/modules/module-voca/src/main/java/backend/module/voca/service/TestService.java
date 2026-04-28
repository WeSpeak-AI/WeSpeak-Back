package backend.module.voca.service;

import backend.module.voca.dto.TestRecordPreview;
import backend.module.voca.dto.TestResultRequest;
import backend.module.voca.dto.TestingWordResponse;
import backend.module.voca.dto.WordResultResponse;

import java.util.List;

public interface TestService {
    List<TestingWordResponse> startTest(Long bookId, int startDay, int endDay);

    void endTest(Long bookId, TestResultRequest testResultRequest, String email);

    List<TestRecordPreview> getTestRecordPreviews(String email, Long bookId);

    List<WordResultResponse> getIncorrectResult(Long testId);
}
