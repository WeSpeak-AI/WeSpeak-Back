package backend.core.common.event;

import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import backend.core.common.event.payload.TopicUpdateEventPayload;
import backend.core.common.event.payload.UserProfileUpdatedEventPayload;
import backend.core.common.event.payload.UserRegisteredEventPayload;
import backend.core.common.event.payload.UserStatEventPayload;
import backend.core.common.event.payload.VocaGenerationEventPayload;
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
    USER_REGISTERED(UserRegisteredEventPayload.class, EventTopic.USER_LIFECYCLE),
    USER_PROFILE_UPDATED(UserProfileUpdatedEventPayload.class, EventTopic.USER_LIFECYCLE),
    VOCA_BOOK_ENROLLED(UserStatEventPayload.class, EventTopic.USER_STATS),
    ESSAY_SUBMITTED(UserStatEventPayload.class, EventTopic.USER_STATS),
    USER_BOOK_PROGRESSED(UserStatEventPayload.class, EventTopic.USER_STATS),
    CONVERSATION_HELD(UserStatEventPayload.class, EventTopic.USER_STATS);


    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class EventTopic {
        public static final String WRITING = "writing";
        public static final String USER = "user";
        public static final String TOPIC = "topic";
        public static final String VOCA = "voca";
        public static final String USER_LIFECYCLE = "user-lifecycle";
        public static final String USER_STATS = "user-stats";
    }
}
