package backend.module.writing.consumer.eventhandler;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.handler.EventHandler;
import backend.core.common.event.payload.TopicUpdateEventPayload;
import backend.module.writing.domain.TopicSummary;
import backend.module.writing.repository.TopicSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicSummaryHandler implements EventHandler<TopicUpdateEventPayload> {
    private final TopicSummaryRepository topicSummaryRepository;

    @Override
    @Transactional
    public void handle(Event<TopicUpdateEventPayload> event) {
        TopicUpdateEventPayload payload = event.getPayload();

        TopicSummary topicSummary = topicSummaryRepository.findById(payload.getTopicId())
                .orElseGet(() -> TopicSummary.builder()
                        .topicId(payload.getTopicId())
                        .title(payload.getTitle())
                        .build());
        topicSummary.updateTitle(payload.getTitle());
        topicSummaryRepository.save(topicSummary);
    }

    @Override
    public boolean supports(Event<TopicUpdateEventPayload> event) {
        return event.getEventType() == EventType.TOPIC_UPDATE;
    }
}
