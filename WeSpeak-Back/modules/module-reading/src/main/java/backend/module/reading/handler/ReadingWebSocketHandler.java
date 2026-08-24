package backend.module.reading.handler;

import backend.module.reading.service.ReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.Disposable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadingWebSocketHandler extends AbstractWebSocketHandler {

    private static final String AUDIO_BUFFER = "audioBuffer";
    private static final String ACTIVE_FEEDBACK_SUBSCRIPTION = "activeFeedbackSubscription";

    private final ReadingService readingService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.getAttributes().put(AUDIO_BUFFER, new ByteArrayOutputStream());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteArrayOutputStream buffer = getBuffer(session);
        try {
            buffer.write(message.getPayload().array());
        } catch (IOException e) {
            log.error("[ReadingWebSocketHandler] buffer write error: {}", e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (!"END".equals(message.getPayload())) return;

        ByteArrayOutputStream buffer = getBuffer(session);
        if (buffer.size() == 0) return;

        String email = (String) session.getAttributes().get("email");
        Long bookPageId = extractBookPageId(session);
        byte[] audioBytes = buffer.toByteArray();
        buffer.reset();

        Disposable subscription = readingService.processUserSummary(email, bookPageId, audioBytes)
                .subscribe(
                        response -> {
                            try {
                                session.sendMessage(new TextMessage(response.userText()));
                                session.sendMessage(new TextMessage(response.feedbackText()));
                            } catch (IOException e) {
                                log.error("[ReadingWebSocketHandler] failed to send message: {}", e.getMessage());
                            }
                        },
                        error -> {
                            log.error("[ReadingWebSocketHandler] AI feedback call failed: {}", error.getMessage());
                            try {
                                session.close(CloseStatus.SERVER_ERROR);
                            } catch (IOException e) {
                                log.error("[ReadingWebSocketHandler] failed to close session after error: {}", e.getMessage());
                            }
                        }
                );

        session.getAttributes().put(ACTIVE_FEEDBACK_SUBSCRIPTION, subscription);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        disposeActiveFeedbackSubscription(session);
        session.getAttributes().remove(AUDIO_BUFFER);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[ReadingWebSocketHandler] error: {}", exception.getMessage());
        session.getAttributes().remove(AUDIO_BUFFER);
    }

    private void disposeActiveFeedbackSubscription(WebSocketSession session) {
        Object attribute = session.getAttributes().get(ACTIVE_FEEDBACK_SUBSCRIPTION);
        if (attribute instanceof Disposable disposable && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private ByteArrayOutputStream getBuffer(WebSocketSession session) {
        return (ByteArrayOutputStream) session.getAttributes().get(AUDIO_BUFFER);
    }

    private Long extractBookPageId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf('/') + 1));
    }
}
