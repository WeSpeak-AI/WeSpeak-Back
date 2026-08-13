package backend.module.voca.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.ImageGenerationEventPayload;
import backend.module.voca.domain.Word;
import backend.module.voca.consumer.VocaSaveService;
import backend.module.voca.dto.ImageGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerationHandler implements EventHandler<ImageGenerationEventPayload> {

    private static final int CHUNK_SIZE = 20;

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final VocaSaveService vocaSaveService;

    @Override
    public void handle(Event<ImageGenerationEventPayload> event) {
        Long vocaBookId = event.getPayload().getVocaBookId();

        List<Word> words = vocaSaveService.getWords(vocaBookId);
        if (words.isEmpty()) {
            log.warn("[ImageGenerationHandler] 단어 없음. vocaBookId={}", vocaBookId);
            return;
        }

        List<List<Word>> chunks = partition(words, CHUNK_SIZE);
        log.info("[ImageGenerationHandler] 시작. vocaBookId={}, 총 {}개 단어, {}개 청크",
                vocaBookId, words.size(), chunks.size());

        int successCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            List<Word> chunk = chunks.get(i);
            try {
                List<Map<String, Object>> body = chunk.stream()
                        .map(w -> Map.<String, Object>of("wordId", w.getWordId(), "term", w.getTerm()))
                        .toList();

                ImageGenerationResponse response = aiWebClient.post()
                        .uri("/voca/word/image")
                        .bodyValue(Map.of("words", body))
                        .retrieve()
                        .bodyToMono(ImageGenerationResponse.class)
                        .timeout(Duration.ofMinutes(10))
                        .block();

                if (response != null && response.results() != null) {
                    vocaSaveService.saveWordImages(response.results());
                    successCount += response.results().size();
                }

                log.info("[ImageGenerationHandler] 청크 {}/{} 완료 ({}개)", i + 1, chunks.size(), chunk.size());
            } catch (Exception e) {
                log.error("[ImageGenerationHandler] 청크 {}/{} 실패: {}", i + 1, chunks.size(), e.getMessage());
            }
        }

        log.info("[ImageGenerationHandler] 완료. vocaBookId={}, 성공={}/{}", vocaBookId, successCount, words.size());
    }

    @Override
    public boolean supports(Event<ImageGenerationEventPayload> event) {
        return EventType.IMAGE_GENERATION == event.getEventType();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        return IntStream.range(0, (list.size() + size - 1) / size)
                .mapToObj(i -> list.subList(i * size, Math.min((i + 1) * size, list.size())))
                .toList();
    }
}
