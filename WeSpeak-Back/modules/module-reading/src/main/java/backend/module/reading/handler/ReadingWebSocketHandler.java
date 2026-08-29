package backend.module.reading.consumer.eventhandler;

import backend.core.common.dataserializer.DataSerializer;
import backend.core.common.exception.BusinessException;
import backend.core.grpc.ai.v1.FeedbackChunk;
import backend.module.reading.service.ReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.Disposable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

        AtomicBoolean errorOccurred = new AtomicBoolean(false);

        // consumeTicket(email)이 Flux 체인 밖에서 동기적으로 BusinessException을 던질 수 있어
        // (예: 티켓 소진) subscribe() 이전에 예외가 발생한다 — .subscribe()의 에러 콜백으로는 잡히지
        // 않으므로 여기서 직접 잡아 동일한 에러 처리 경로(handleFeedbackFailure)로 보낸다.
        try {
            Disposable subscription = readingService.processUserSummary(email, bookPageId, audioBytes)
                    .subscribe(
                            chunk -> handleFeedbackChunk(session, chunk, errorOccurred),
                            error -> handleFeedbackFailure(session, error.getMessage(), errorOccurred),
                            () -> handleFeedbackComplete(session, errorOccurred)
                    );
            session.getAttributes().put(ACTIVE_FEEDBACK_SUBSCRIPTION, subscription);
        } catch (BusinessException e) {
            handleFeedbackFailure(session, e.getMessage(), errorOccurred);
        }
    }

    private void handleFeedbackChunk(WebSocketSession session, FeedbackChunk chunk, AtomicBoolean errorOccurred) {
        try {
            switch (chunk.getPayloadCase()) {
                case USER_TEXT_FINAL -> session.sendMessage(new TextMessage(DataSerializer.serialize(
                        Map.of("type", "user_text", "text", chunk.getUserTextFinal()))));
                case FEEDBACK_TEXT_DELTA -> session.sendMessage(new TextMessage(DataSerializer.serialize(
                        Map.of("type", "feedback_text_delta", "text", chunk.getFeedbackTextDelta()))));
                case STREAM_ERROR -> handleFeedbackFailure(session, chunk.getStreamError().getMessage(), errorOccurred);
                case PAYLOAD_NOT_SET -> log.warn("[ReadingWebSocketHandler] empty FeedbackChunk received");
            }
        } catch (IOException e) {
            log.error("[ReadingWebSocketHandler] failed to send message: {}", e.getMessage());
        }
    }

    private void handleFeedbackFailure(WebSocketSession session, String message, AtomicBoolean errorOccurred) {
        if (!errorOccurred.compareAndSet(false, true)) {
            return;
        }
        log.error("[ReadingWebSocketHandler] AI feedback stream failed: {}", message);
        try {
            session.sendMessage(new TextMessage(DataSerializer.serialize(
                    Map.of("type", "error", "message", message))));
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.error("[ReadingWebSocketHandler] failed to notify/close session after error: {}", e.getMessage());
        }
    }

    private void handleFeedbackComplete(WebSocketSession session, AtomicBoolean errorOccurred) {
        if (errorOccurred.get()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(DataSerializer.serialize(Map.of("type", "done"))));
        } catch (IOException e) {
            log.error("[ReadingWebSocketHandler] failed to send done signal: {}", e.getMessage());
        }
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
