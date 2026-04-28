package backend.module.writing.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.writing.Essay;
import backend.module.writing.repository.EssayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiCorrectionHandler implements EventHandler<AiCorrectionEventPayload> {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final EssayRepository essayRepository;

    @Override
    @Transactional
    public void handle(Event<AiCorrectionEventPayload> event) {
        AiCorrectionEventPayload payload = event.getPayload();

        String correctionResult = aiWebClient.post()
                .uri("/correct")
                .bodyValue(Map.of("content", payload.getContent()))
                .retrieve()
                .bodyToMono(AiCorrectionResponse.class)
                .map(AiCorrectionResponse::getResult)
                .block();

        Essay essay = essayRepository.findById(payload.getEssayId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ESSAY_NOT_FOUND));
        essay.applyCorrection(correctionResult);
    }

    @Override
    public boolean supports(Event<AiCorrectionEventPayload> event) {
        return EventType.AI_CORRECTION == event.getEventType();
    }

    private record AiCorrectionResponse(String result) {
        public String getResult() { return result; }
    }
}
