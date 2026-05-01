package backend.module.reading.controller;

import backend.core.common.response.ApiResponse;
import backend.core.domain.reading.Book.Level;
import backend.module.reading.dto.ReadingBookContent;
import backend.module.reading.dto.ReadingBookPreviewResponse;
import backend.module.reading.dto.ReadingRequest;
import backend.module.reading.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @GetMapping("/books")
    public ApiResponse<Page<ReadingBookPreviewResponse>> getAllBooks(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                     @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(readingService.getAllBooks(page, size));
    }

    @GetMapping("/books/{level}")
    public ApiResponse<Page<ReadingBookPreviewResponse>> getBooksByLevel(@PathVariable("level") Level level,
                                                                         @RequestParam(name = "page", defaultValue = "0") int page,
                                                                         @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(readingService.getBooksByLevel(level, page, size));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/start")
    public ApiResponse<Long> startBook(@RequestHeader("X-Username") String email, @Valid @RequestBody ReadingRequest readingRequest) {
        return ApiResponse.ok(readingService.startBook(email, readingRequest));
    }

    @GetMapping("/books/myBook")
    public ApiResponse<List<ReadingBookPreviewResponse>> getMyBooks(@RequestHeader("X-Username") String email) {
        return ApiResponse.ok(readingService.getMyBooks(email));
    }

    @GetMapping("/books/{bookId}/pages")
    public ApiResponse<ReadingBookContent> getPage(
            @RequestHeader("X-Username") String email,
            @PathVariable("bookId") Long bookId,
            @RequestParam(name = "pageNumber", required = false) Integer pageNumber
    ) {
        return ApiResponse.ok(readingService.getPage(email, bookId, pageNumber));
    }

    @DeleteMapping("/books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyBook(@PathVariable("bookId") Long bookId, @RequestHeader("X-username") String email) {
        readingService.deleteMyBook(bookId, email);
    }

}
