package backend.module.searchword.service;

import backend.module.searchword.dto.WordSearchResponse;

public interface WordSearchService {
    WordSearchResponse search(String query);
}
