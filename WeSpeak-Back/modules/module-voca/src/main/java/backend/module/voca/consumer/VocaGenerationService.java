package backend.module.voca.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;

public interface VocaGenerationService {
    void handleEvent(Event<EventPayload> event);
}
