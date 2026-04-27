package backend.cowrite.common.event;

import backend.cowrite.common.event.payload.DocumentSaveEventPayload;
import backend.cowrite.common.event.payload.DocumentUpdateEventPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    UPDATE(DocumentUpdateEventPayload.class, Topic.UPDATE),
    SAVE(DocumentSaveEventPayload.class, Topic.SAVE);

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class Topic {
        public static final String UPDATE = "content-update";
        public static final String SAVE = "content-save";
    }
}
