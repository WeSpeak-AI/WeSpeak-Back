package backend.module.voca.controller;

import backend.core.common.response.ApiResponse;
import backend.core.domain.voca.VocaBook.Category;
import backend.module.voca.dto.MyVocaBookResponse;
import backend.module.voca.dto.VocaBookDayResponse;
import backend.module.voca.dto.VocaBookResponse;
import backend.module.voca.dto.WordResponse;
import backend.module.voca.service.VocaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voca")
@RequiredArgsConstructor
public class VocaController {

    private final VocaService vocaService;

    @GetMapping("/books")
    public ApiResponse<Page<VocaBookResponse>> getAllVocas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(vocaService.getAllVocas(page, size));
    }

    @GetMapping("/books/category/{category}")
    public ApiResponse<Page<VocaBookResponse>> getVocasByCategory(
            @PathVariable("category") Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(vocaService.getVocasByCategory(category, page, size));
    }

    @PostMapping("/books/{bookId}/start")
    public ApiResponse<Void> startVoca(@RequestHeader("X-Username") String email, @PathVariable("bookId") Long bookId) {
        vocaService.startVoca(email, bookId);
        return ApiResponse.ok();
    }

    @GetMapping("/books/myVoca")
    public ApiResponse<List<MyVocaBookResponse>> getMyVocas(@RequestHeader("X-Username") String email) {
        return ApiResponse.ok(vocaService.getMyVocas(email));
    }

    @GetMapping("/books/{bookId}/days")
    public ApiResponse<List<VocaBookDayResponse>> getAllDaysByBook(@PathVariable("bookId") Long bookId) {
        return ApiResponse.ok(vocaService.getAllDaysByBook(bookId));
    }

    @GetMapping("/books/{bookId}/days/{dayNumber}/words")
    public ApiResponse<List<WordResponse>> getWordsByDay(
            @RequestHeader("X-Username") String email,
            @PathVariable("bookId") Long bookId,
            @PathVariable("dayNumber") int dayNumber
    ) {
        return ApiResponse.ok(vocaService.getWordsByDay(email, bookId, dayNumber));
    }
}
