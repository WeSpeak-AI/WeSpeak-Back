package backend.module.user.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;

public interface StreakUpdateService {
    void handleEvent(Event<EventPayload> event);
}
