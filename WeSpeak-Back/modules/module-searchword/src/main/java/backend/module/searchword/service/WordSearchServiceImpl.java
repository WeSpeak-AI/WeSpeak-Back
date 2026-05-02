package backend.module.searchword.service;

import backend.module.searchword.dto.WordSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WordSearchServiceImpl implements WordSearchService {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;

    @Override
    public WordSearchResponse search(String query) {
        return aiWebClient.post()
                .uri("/search")
                .bodyValue(Map.of("query", query))
                .retrieve()
                .bodyToMono(WordSearchResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }
}
