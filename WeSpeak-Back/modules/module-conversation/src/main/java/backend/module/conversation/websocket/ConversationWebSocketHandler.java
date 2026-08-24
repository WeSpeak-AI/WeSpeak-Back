package backend.module.conversation.websocket;

import backend.core.common.dataserializer.DataSerializer;
import backend.module.conversation.domain.Conversation;
import backend.module.conversation.repository.ConversationMessageRepository;
import backend.module.conversation.service.ConversationMessageService;
import backend.module.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationWebSocketHandler extends AbstractWebSocketHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConversationMessageService conversationMessageService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationService conversationService;
    private final AiClient aiClient;

    private static final String REDIS_KEY = "conversation:history:";
    private static final String ACTIVE_CHAT_SUBSCRIPTION = "activeChatSubscription";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        Conversation conversation = conversationService.getConversation(sessionId);
        if (conversation.getStatus() == Conversation.Status.CLOSED) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = extractSessionId(session);
        String redisKey = REDIS_KEY + sessionId;
        Conversation conversation = conversationService.getConversation(sessionId);

        byte[] audioBytes = message.getPayload().array();
        List<Map<String, String>> history = DataSerializer.deserialize(getHistory(redisKey, conversation), List.class);

        Disposable subscription = aiClient.chat(audioBytes, history)
                .subscribe(
                        response -> {
                            conversationMessageService.saveUserMessageToDB(conversation, response.userText());
                            conversationMessageService.saveAiMessageToDB(conversation, response.aiText());

                            history.add(Map.of("role", "user", "content", response.userText()));
                            history.add(Map.of("role", "assistant", "content", response.aiText()));
                            redisTemplate.opsForValue().set(redisKey, DataSerializer.serialize(history));

                            try {
                                session.sendMessage(new TextMessage(response.userText()));
                                session.sendMessage(new TextMessage(response.aiText()));
                                session.sendMessage(new BinaryMessage(Base64.getDecoder().decode(response.audioData())));
                            } catch (IOException e) {
                                log.error("[ConversationWebSocketHandler] failed to send message: {}", e.getMessage());
                            }
                        },
                        error -> {
                            log.error("[ConversationWebSocketHandler] AI chat call failed: {}", error.getMessage());
                            try {
                                session.close(CloseStatus.SERVER_ERROR);
                            } catch (IOException e) {
                                log.error("[ConversationWebSocketHandler] failed to close session after error: {}", e.getMessage());
                            }
                        }
                );

        session.getAttributes().put(ACTIVE_CHAT_SUBSCRIPTION, subscription);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        disposeActiveChatSubscription(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[ConversationWebSocketHandler] error: {}", exception.getMessage());
    }

    private void disposeActiveChatSubscription(WebSocketSession session) {
        Object attribute = session.getAttributes().get(ACTIVE_CHAT_SUBSCRIPTION);
        if (attribute instanceof Disposable disposable && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private String getHistory(String redisKey, Conversation conversation) {
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            return (String) cached;
        }

        List<Map<String, String>> history = conversationMessageRepository
                .findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(msg -> Map.of(
                        "role", msg.getRole().name().toLowerCase(),
                        "content", msg.getContent()
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        String serialized = DataSerializer.serialize(history);
        redisTemplate.opsForValue().set(redisKey, serialized);
        return serialized;
    }

    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
