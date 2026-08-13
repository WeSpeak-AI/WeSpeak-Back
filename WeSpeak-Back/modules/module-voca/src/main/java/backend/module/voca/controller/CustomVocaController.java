package backend.module.voca.controller;

import backend.core.common.response.ApiResponse;
import backend.module.voca.dto.CustomVocaBookRequest;
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

    @PostMapping
    public ApiResponse<CustomVocaBookResponse> createCustomVocaBook(
            @RequestHeader("X-Username") String email,
            @RequestBody CustomVocaBookRequest request
    ) {
        return ApiResponse.ok(customVocaService.createCustomVocaBook(email, request));
    }

    @GetMapping
    public ApiResponse<List<CustomVocaBookResponse>> getMyCustomVocaBooks(
            @RequestHeader("X-Username") String email
    ) {
        return ApiResponse.ok(customVocaService.getMyCustomVocaBooks(email));
    }

    @DeleteMapping("/{customVocaBookId}")
    public ApiResponse<Void> deleteCustomVocaBook(
            @RequestHeader("X-Username") String email,
            @PathVariable Long customVocaBookId
    ) {
        customVocaService.deleteCustomVocaBook(email, customVocaBookId);
        return ApiResponse.ok();
    }

    @PostMapping("/{customVocaBookId}/words")
    public ApiResponse<CustomWordResponse> addWord(
            @RequestHeader("X-Username") String email,
            @PathVariable Long customVocaBookId,
            @RequestBody CustomWordRequest request
    ) {
        return ApiResponse.ok(customVocaService.addWord(email, customVocaBookId, request));
    }

    @GetMapping("/{customVocaBookId}/words")
    public ApiResponse<List<CustomWordResponse>> getWords(
            @RequestHeader("X-Username") String email,
            @PathVariable Long customVocaBookId
    ) {
        return ApiResponse.ok(customVocaService.getWords(email, customVocaBookId));
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
