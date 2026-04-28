package backend.module.voca.controller;

import backend.core.common.response.ApiResponse;
import backend.core.webclient.tts.TtsService;
import backend.module.voca.dto.TestRecordPreview;
import backend.module.voca.dto.TestResultRequest;
import backend.module.voca.dto.TestingWordResponse;
import backend.module.voca.dto.TestingDayRequest;
import backend.module.voca.dto.WordResultResponse;
import backend.module.voca.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final TtsService ttsService;

    //todo: 시험 범위 중 랜덤한 몇 개만 테스트
    // 시험 범위 중 전에 틀린 단어가 포함되어 있다면 해당 단어는 무조건 테스트
    @PostMapping("/{bookId}/start")
    public ApiResponse<List<TestingWordResponse>> startTest(@RequestBody @Valid TestingDayRequest testingDay,
                                                            @PathVariable("bookId") Long bookId) {
        List<TestingWordResponse> testingWords = testService.startTest(bookId, testingDay.getStartDay(), testingDay.getEndDay());
        return ApiResponse.ok(testingWords);
    }

    @PostMapping("/{bookId}/end")
    public ApiResponse<Void> endTest(@RequestBody @Valid TestResultRequest testResultRequest, @PathVariable("bookId") Long bookId,
                                     @RequestHeader("X-Username") String email) {
        testService.endTest(bookId, testResultRequest, email);
        return ApiResponse.ok();
    }

    @GetMapping("/mytest/list/{bookId}")
    public ApiResponse<List<TestRecordPreview>> getTestRecords(@RequestHeader("X-Username") String email,
                                                               @PathVariable("bookId") Long bookId ) {
        List<TestRecordPreview> testRecordPreviews = testService.getTestRecordPreviews(email, bookId);
        return ApiResponse.ok(testRecordPreviews);
    }

    @GetMapping("/mytest/{testId}")
    public ApiResponse<List<WordResultResponse>> getIncorrectResult(@PathVariable("testId") Long testId) {
        return ApiResponse.ok(testService.getIncorrectResult(testId));
    }

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(@RequestParam("text") String text) {
        StreamingResponseBody body = outputStream -> ttsService.stream(text, outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}
