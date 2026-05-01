package backend.module.voca.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.VocaGenerationEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.voca.VocaBook;
import backend.core.domain.voca.VocaBookDay;
import backend.core.domain.voca.Word;
import backend.core.infra.Snowflake;
import backend.module.voca.dto.VocaGenerationResponse;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
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
public class VocaGenerationEventHandler implements EventHandler<VocaGenerationEventPayload> {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final VocaBookRepository vocaBookRepository;
    private final VocaBookDayRepository vocaBookDayRepository;
    private final WordRepository wordRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
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
                .block();

        if (response == null || response.days() == null) {
            log.error("[VocaGenerationEventHandler] AI 응답이 없습니다. vocaId={}", payload.getVocaId());
            return;
        }

        VocaBook vocaBook = vocaBookRepository.findById(payload.getVocaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));

        for (VocaGenerationResponse.DayResult dayResult : response.days()) {
            VocaBookDay vocaBookDay = VocaBookDay.builder()
                    .vocaBookDayId(snowflake.nextId())
                    .vocaBook(vocaBook)
                    .day(dayResult.day())
                    .dayTopic(dayResult.dayTopic())
                    .build();
            vocaBookDayRepository.save(vocaBookDay);
            vocaBook.incrementTotalDays();

            for (VocaGenerationResponse.WordResult wordResult : dayResult.words()) {
                wordRepository.save(Word.builder()
                        .wordId(snowflake.nextId())
                        .vocaBookDay(vocaBookDay)
                        .term(wordResult.term())
                        .meaning(wordResult.meaning())
                        .phonetic(wordResult.phonetic())
                        .example(wordResult.example())
                        .imageUrl(wordResult.imageUrl())
                        .build());
            }
        }
    }

    @Override
    public boolean supports(Event<VocaGenerationEventPayload> event) {
        return EventType.VOCA_GENERATION == event.getEventType();
    }
}
