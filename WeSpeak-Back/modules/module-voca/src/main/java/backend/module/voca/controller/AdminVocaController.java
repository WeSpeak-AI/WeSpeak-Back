package backend.module.voca.controller;

import backend.core.common.response.ApiResponse;
import backend.module.voca.dto.*;
import backend.module.voca.service.AdminVocaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminVocaController {

    private final AdminVocaService adminVocaService;

    @PostMapping("/voca-books")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createVocaBook(@Valid @RequestBody VocaBookRequest request) {
        return ApiResponse.ok(adminVocaService.createVocaBook(request));
    }

    @PutMapping("/voca-books/{vocaBookId}")
    public ApiResponse<Void> updateVocaBook(@PathVariable("vocaBookId") Long vocaBookId, @Valid @RequestBody VocaBookRequest request) {
        adminVocaService.updateVocaBook(vocaBookId, request);
        return ApiResponse.ok();
    }

    @PatchMapping("/voca-books/{vocaBookId}/image")
    public ApiResponse<String> uploadVocaBookImage(@PathVariable("vocaBookId") Long vocaBookId,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminVocaService.uploadVocaBookImage(vocaBookId, file));
    }

    @DeleteMapping("/voca-books/{vocaBookId}")
    public ApiResponse<Void> deleteVocaBook(@PathVariable("vocaBookId") Long vocaBookId) {
        adminVocaService.deleteVocaBook(vocaBookId);
        return ApiResponse.ok();
    }

    @PostMapping("/voca-books/{vocaBookId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createVocaBookDay(@PathVariable("vocaBookId") Long vocaBookId, @Valid @RequestBody VocaBookDayRequest request) {
        return ApiResponse.ok(adminVocaService.createVocaBookDay(vocaBookId, request));
    }

    @PutMapping("/days/{vocaBookDayId}")
    public ApiResponse<Void> updateVocaBookDay(@PathVariable("vocaBookDayId") Long vocaBookDayId, @Valid @RequestBody VocaBookDayRequest request) {
        adminVocaService.updateVocaBookDay(vocaBookDayId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/days/{vocaBookDayId}")
    public ApiResponse<Void> deleteVocaBookDay(@PathVariable("vocaBookDayId") Long vocaBookDayId) {
        adminVocaService.deleteVocaBookDay(vocaBookDayId);
        return ApiResponse.ok();
    }

    @PostMapping("/days/{vocaBookDayId}/words")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createWord(@PathVariable("vocaBookDayId") Long vocaBookDayId, @Valid @RequestBody WordRequest request) {
        return ApiResponse.ok(adminVocaService.createWord(vocaBookDayId, request));
    }

    @PutMapping("/words/{wordId}")
    public ApiResponse<Void> updateWord(@PathVariable("wordId") Long wordId, @Valid @RequestBody WordRequest request) {
        adminVocaService.updateWord(wordId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/words/{wordId}")
    public ApiResponse<Void> deleteWord(@PathVariable("wordId") Long wordId) {
        adminVocaService.deleteWord(wordId);
        return ApiResponse.ok();
    }

    @PostMapping("/voca/ai-generate")
    public ApiResponse<Void> aiGenerate(@Valid @RequestBody VocaBookRequest request) {
        adminVocaService.aiGenerate(request);
        return ApiResponse.ok();
    }

    @PostMapping("/voca/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> ingestVoca(@Valid @RequestBody IngestRequest request) {
        adminVocaService.ingestVoca(request);
        return ApiResponse.ok();
    }
}
