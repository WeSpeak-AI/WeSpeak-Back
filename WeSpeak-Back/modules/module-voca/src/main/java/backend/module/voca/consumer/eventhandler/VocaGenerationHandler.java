package backend.module.voca.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.VocaGenerationEventPayload;
import backend.core.infra.Snowflake;
import backend.module.voca.consumer.VocaSaveService;
import backend.module.voca.dto.VocaGenerationResponse;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VocaGenerationHandler implements EventHandler<VocaGenerationEventPayload> {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final VocaSaveService vocaSaveService;

    @Override
    public void handle(Event<VocaGenerationEventPayload> event) {
        VocaGenerationEventPayload payload = event.getPayload();

        VocaGenerationResponse response = aiWebClient.post()
                .uri("/voca")
                .bodyValue(Map.of(
                        "title", payload.getTitle(),
                        "category", payload.getCategory(),
                        "description", payload.getDescription(),
                        "numberOfDays", payload.getNumberOfDays()
                ))
                .retrieve()
                .bodyToMono(VocaGenerationResponse.class)
                .timeout(Duration.ofDays(1))
                .block();

        if (response == null || response.days() == null) {
            log.error("[VocaGenerationEventHandler] AI 응답이 없습니다. vocaId={}", payload.getVocaId());
            return;
        }

        vocaSaveService.saveGeneratedVoca(payload.getVocaId(), response);
    }

    @Override
    public boolean supports(Event<VocaGenerationEventPayload> event) {
        return EventType.VOCA_GENERATION == event.getEventType();
    }
}
