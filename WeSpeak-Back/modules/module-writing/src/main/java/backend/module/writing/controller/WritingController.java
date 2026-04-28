package backend.module.writing.controller;

import backend.core.common.response.ApiResponse;
import backend.module.writing.dto.CorrectionResponse;
import backend.module.writing.dto.EssayRequest;
import backend.module.writing.dto.EssayResponse;
import backend.module.writing.service.WritingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;

    @GetMapping("/topic")
    public ApiResponse<String> getRandomTopic() {
        return ApiResponse.ok(writingService.getRandomTopic());
    }

    @PostMapping("/essays")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EssayResponse> saveEssay(
            @RequestHeader("X-Username") String email,
            @Valid @RequestBody EssayRequest request
    ) {
        return ApiResponse.ok(writingService.save(email, request));
    }

    @GetMapping("/essays")
    public ApiResponse<List<EssayResponse>> getMyEssays(
            @RequestHeader("X-Username") String email
    ) {
        return ApiResponse.ok(writingService.getMyEssays(email));
    }

    @GetMapping("/essays/{essayId}")
    public ApiResponse<CorrectionResponse> getMyEssay(@PathVariable("essayId") Long essayId) {
        return ApiResponse.ok(writingService.getMyEssay(essayId));
    }
}
