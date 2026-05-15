package backend.module.reading.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;
import backend.core.common.event.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDeletedServiceImpl implements UserDeletedService {
    private final List<EventHandler> eventHandlers;

    @Override
    @SuppressWarnings("unchecked")
    public void handleEvent(Event<EventPayload> event) {
        EventHandler<EventPayload> eventHandler = (EventHandler<EventPayload>) eventHandlers.stream()
                .filter(handler -> handler.supports(event))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("해당하는 이벤트 핸들러를 찾지 못했습니다."));
        eventHandler.handle(event);
    }
}
