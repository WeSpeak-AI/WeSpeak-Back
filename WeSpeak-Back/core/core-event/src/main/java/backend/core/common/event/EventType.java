package backend.core.common.event;

import backend.core.common.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    AI_CORRECTION(AiCorrectionEventPayload.class, EventTopic.WRITING),
    STUDY_COMPLETED(StudyCompletedEventPayload.class, EventTopic.USER),
    TOPIC_UPDATE(TopicUpdateEventPayload.class, EventTopic.TOPIC),
    VOCA_GENERATION(VocaGenerationEventPayload.class, EventTopic.VOCA),
    IMAGE_GENERATION(ImageGenerationEventPayload.class, EventTopic.IMAGE),
    USER_DELETED(UserDeletedEventPayload.class, EventTopic.USER_DELETED);


    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class EventTopic {
        public static final String WRITING = "writing";
        public static final String USER = "user";
        public static final String TOPIC = "topic";
        public static final String VOCA = "voca";
        public static final String IMAGE = "image";
        public static final String USER_DELETED = "user-deleted";
    }
}
