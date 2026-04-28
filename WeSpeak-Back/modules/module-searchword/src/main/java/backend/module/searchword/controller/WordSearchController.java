package backend.module.searchword.controller;

import backend.core.common.response.ApiResponse;
import backend.module.searchword.dto.WordSearchResponse;
import backend.module.searchword.service.WordSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class WordSearchController {

    private final WordSearchService wordSearchService;

    @GetMapping("/word")
    public ApiResponse<WordSearchResponse> searchWord(@RequestParam("query") String query) {
        return ApiResponse.ok(wordSearchService.search(query));
    }
}
