package backend.core.common.event;

import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.event.payload.StudyCompletedEventPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    AI_CORRECTION(AiCorrectionEventPayload.class, Topic.WRITING),
    STUDY_COMPLETED(StudyCompletedEventPayload.class, Topic.USER);


    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class Topic {
        public static final String WRITING = "writing";
        public static final String USER = "user";
    }
}
