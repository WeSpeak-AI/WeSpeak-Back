package backend.cowrite.common.event;

import backend.cowrite.common.dataserializer.DataSerializer;
import lombok.Getter;

@Getter
public class Event <T extends EventPayload> {
    private Long eventId;
    private EventType eventType;
    private T payload;

    public static Event<EventPayload> createEvent(Long eventId, EventType eventType, EventPayload payload) {
        Event<EventPayload> newEvent = new Event<>();
        newEvent.eventId = eventId;
        newEvent.eventType = eventType;
        newEvent.payload = payload;
        return newEvent;
    }

    public String toJson() {
        return DataSerializer.serialize(this);
    }

    public static Event<EventPayload> fromJson(String json) {
        EventRaw eventRaw = DataSerializer.deserialize(json, EventRaw.class);
        if(eventRaw == null) {
            return null;
        }
        Event<EventPayload> event = new Event<>();
        event.eventId = eventRaw.getEventId();
        event.eventType = eventRaw.getEventType();
        event.payload = DataSerializer.deserialize(eventRaw.payload,event.eventType.getPayloadClass());

        return event;
    }

    @Getter
    private static class EventRaw {
        private Long eventId;
        private EventType eventType;
        private Object payload;
    }
}
