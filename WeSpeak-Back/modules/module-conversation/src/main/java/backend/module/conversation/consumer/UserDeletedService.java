package backend.module.conversation.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;

public interface UserDeletedService {
    void handleEvent(Event<EventPayload> event);
}
