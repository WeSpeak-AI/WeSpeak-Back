package backend.module.reading.handler;

import backend.module.reading.dto.ReadingAiResponse;
import backend.module.reading.service.ReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadingWebSocketHandler extends AbstractWebSocketHandler {

    private static final String AUDIO_BUFFER = "audioBuffer";

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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!"END".equals(message.getPayload())) return;

        ByteArrayOutputStream buffer = getBuffer(session);
        if (buffer.size() == 0) return;

        String email = (String) session.getAttributes().get("email");
        Long bookPageId = extractBookPageId(session);
        byte[] audioBytes = buffer.toByteArray();
        buffer.reset();

        ReadingAiResponse response = readingService.processUserSummary(email, bookPageId, audioBytes);

        session.sendMessage(new TextMessage(response.userText()));
        session.sendMessage(new TextMessage(response.feedbackText()));
        session.sendMessage(new BinaryMessage(Base64.getDecoder().decode(response.audioData())));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        session.getAttributes().remove(AUDIO_BUFFER);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[ReadingWebSocketHandler] error: {}", exception.getMessage());
        session.getAttributes().remove(AUDIO_BUFFER);
    }

    private ByteArrayOutputStream getBuffer(WebSocketSession session) {
        return (ByteArrayOutputStream) session.getAttributes().get(AUDIO_BUFFER);
    }

    private Long extractBookPageId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf('/') + 1));
    }
}
