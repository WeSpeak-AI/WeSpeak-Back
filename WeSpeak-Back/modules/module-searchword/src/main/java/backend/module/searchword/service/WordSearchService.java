package backend.module.searchword.service;

import backend.module.searchword.dto.WordSearchResponse;
import reactor.core.publisher.Mono;

public interface WordSearchService {
    Mono<WordSearchResponse> search(String query);
}
