package backend.module.conversation.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.TopicUpdateEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.topic.Topic;
import backend.core.infra.repository.TopicRepository;
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
public class TopicUpdateHandler implements EventHandler<TopicUpdateEventPayload> {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public void handle(Event<TopicUpdateEventPayload> event) {
        TopicUpdateEventPayload payload = event.getPayload();

        String correctionResult = aiWebClient.post()
                .uri("/topic")
                .bodyValue(Map.of("content", payload.getContent()))
                .retrieve()
                .bodyToMono(TopicnResponse.class)
                .map(TopicnResponse::getResult)
                .block();

        Topic topic = topicRepository.findById(payload.getTopicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.applyContent(correctionResult);
    }

    @Override
    public boolean supports(Event<TopicUpdateEventPayload> event) {
        return EventType.TOPIC_UPDATE == event.getEventType();
    }

    private record TopicnResponse(String result) {
        public String getResult() {
            return result;
        }
    }
}
