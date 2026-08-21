package backend.module.conversation.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.TopicUpdateEventPayload;
import backend.module.conversation.consumer.TopicSaveService;
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
public class TopicUpdateHandler implements EventHandler<TopicUpdateEventPayload> {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final TopicSaveService topicSaveService;

    //Todo: gRPC 전환은 우선순위 낮음 (단순 unary 호출)
    @Override
    public void handle(Event<TopicUpdateEventPayload> event) {
        TopicUpdateEventPayload payload = event.getPayload();

        String correctionResult = aiWebClient.post()
                .uri("/topic")
                .bodyValue(Map.of(
                        "title", payload.getTitle(),
                        "content", payload.getContent(),
                        "difficulty", payload.getDifficulty()
                ))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        topicSaveService.applyContent(payload.getTopicId(), correctionResult);
    }

    @Override
    public boolean supports(Event<TopicUpdateEventPayload> event) {
        return EventType.TOPIC_UPDATE == event.getEventType();
    }
}
