package backend.module.voca.controller;

import backend.core.common.response.ApiResponse;
import backend.module.voca.dto.CustomVocaBookResponse;
import backend.module.voca.dto.CustomWordRequest;
import backend.module.voca.dto.CustomWordResponse;
import backend.module.voca.service.CustomVocaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/custom-voca")
@RequiredArgsConstructor
public class CustomVocaController {

    private final CustomVocaService customVocaService;

    @GetMapping
    public ApiResponse<CustomVocaBookResponse> getMyCustomVocaBook(
            @RequestHeader("X-Username") String email
    ) {
        return ApiResponse.ok(customVocaService.getMyCustomVocaBook(email));
    }

    @PostMapping("/words")
    public ApiResponse<CustomWordResponse> addWord(
            @RequestHeader("X-Username") String email,
            @RequestBody CustomWordRequest request
    ) {
        return ApiResponse.ok(customVocaService.addWord(email, request));
    }

    @GetMapping("/words")
    public ApiResponse<List<CustomWordResponse>> getWords(
            @RequestHeader("X-Username") String email
    ) {
        return ApiResponse.ok(customVocaService.getWords(email));
    }

    @PutMapping("/words/{customWordId}")
    public ApiResponse<CustomWordResponse> updateWord(
            @RequestHeader("X-Username") String email,
            @PathVariable Long customWordId,
            @RequestBody CustomWordRequest request
    ) {
        return ApiResponse.ok(customVocaService.updateWord(email, customWordId, request));
    }

    @DeleteMapping("/words/{customWordId}")
    public ApiResponse<Void> deleteWord(
            @RequestHeader("X-Username") String email,
            @PathVariable Long customWordId
    ) {
        customVocaService.deleteWord(email, customWordId);
        return ApiResponse.ok();
    }
}
