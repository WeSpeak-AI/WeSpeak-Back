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

    //Todo: .subscribe()로 논블로킹 전환, 시나리오 부하테스트
    //Todo: gRPC는 단순 request-response라 이득 작음 (우선순위 낮음)
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
