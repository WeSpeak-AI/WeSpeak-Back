package backend.core.common.event.handler;


import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;

public interface EventHandler<T extends EventPayload> {
    void handle(Event<T> event);
    boolean supports(Event<T> event);
}
