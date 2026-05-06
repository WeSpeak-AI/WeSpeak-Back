package backend.module.reading.controller;

import backend.core.common.response.ApiResponse;
import backend.module.reading.dto.BookPageRequest;
import backend.module.reading.dto.BookRequest;
import backend.module.reading.dto.QuickBookRequest;
import backend.module.reading.service.AdminBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBookController {

    private final AdminBookService adminBookService;

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createBook(@Valid @RequestBody BookRequest request) {
        return ApiResponse.ok(adminBookService.createBook(request));
    }

    @PostMapping("/books/quick-create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> quickCreateBook(@Valid @RequestBody QuickBookRequest request) {
        return ApiResponse.ok(adminBookService.quickCreateBook(request));
    }

    @PutMapping("/books/{bookId}")
    public ApiResponse<Void> updateBook(@PathVariable("bookId") Long bookId, @Valid @RequestBody BookRequest request) {
        adminBookService.updateBook(bookId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/books/{bookId}")
    public ApiResponse<Void> deleteBook(@PathVariable("bookId") Long bookId) {
        adminBookService.deleteBook(bookId);
        return ApiResponse.ok();
    }

    @PostMapping("/books/{bookId}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createBookPage(@PathVariable("bookId") Long bookId, @Valid @RequestBody BookPageRequest request) {
        return ApiResponse.ok(adminBookService.createBookPage(bookId, request));
    }

    @PutMapping("/pages/{bookPageId}")
    public ApiResponse<Void> updateBookPage(@PathVariable("bookPageId") Long bookPageId, @Valid @RequestBody BookPageRequest request) {
        adminBookService.updateBookPage(bookPageId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/pages/{bookPageId}")
    public ApiResponse<Void> deleteBookPage(@PathVariable("bookPageId") Long bookPageId) {
        adminBookService.deleteBookPage(bookPageId);
        return ApiResponse.ok();
    }
}
