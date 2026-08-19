package backend.core.common.event.handler;

import backend.core.common.event.Event;
import backend.core.common.event.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 여러 이벤트 타입이 같은 토픽에 실리는 경우(예: user-lifecycle) 구독 서비스가 그중 일부만
 * 처리해도 되도록, 매칭되는 핸들러가 없으면 예외 없이 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {
    private final List<EventHandler> eventHandlers;

    @SuppressWarnings("unchecked")
    public void dispatch(Event<EventPayload> event) {
        eventHandlers.stream()
                .filter(handler -> handler.supports(event))
                .findAny()
                .ifPresentOrElse(
                        handler -> ((EventHandler<EventPayload>) handler).handle(event),
                        () -> log.debug("[EventDispatcher] no handler for eventType={}, skipping", event.getEventType())
                );
    }
}
