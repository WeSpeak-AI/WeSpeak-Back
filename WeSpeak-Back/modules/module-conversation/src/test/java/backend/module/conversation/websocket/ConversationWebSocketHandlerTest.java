package backend.module.conversation.websocket;

import backend.module.conversation.domain.Conversation;
import backend.module.conversation.repository.ConversationMessageRepository;
import backend.module.conversation.service.ConversationMessageService;
import backend.module.conversation.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * spec FR-009(클라이언트 연결 종료 시 진행 중인 AI 호출 취소) 회귀 검증.
 * AI 서버가 응답하지 않는 상황을 {@link Mono#never()}로 흉내내, WebSocket 세션이 닫히면
 * 진행 중인 구독이 실제로 dispose()되는지 확인한다. Docker/Kafka/AI 서버 없이 결정적으로 검증 가능.
 */
@ExtendWith(MockitoExtension.class)
class ConversationWebSocketHandlerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ConversationMessageService conversationMessageService;
    @Mock
    private ConversationMessageRepository conversationMessageRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private AiClient aiClient;
    @Mock
    private WebSocketSession session;

    @Test
    void disposesInFlightAiCallWhenConnectionCloses() {
        ConversationWebSocketHandler handler = new ConversationWebSocketHandler(
                redisTemplate, conversationMessageService, conversationMessageRepository,
                conversationService, aiClient);

        Conversation conversation = Conversation.builder()
                .conversationId(1L)
                .userEmail("user@test.com")
                .build();

        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/conversation/1"));
        when(conversationService.getConversation("1")).thenReturn(conversation);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(conversationMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(List.of());
        when(aiClient.chat(any(), any())).thenReturn(Mono.never());

        handler.handleBinaryMessage(session, new BinaryMessage(ByteBuffer.wrap(new byte[]{1, 2, 3})));

        Object subscription = attributes.get("activeChatSubscription");
        assertThat(subscription).isInstanceOf(Disposable.class);
        Disposable disposable = (Disposable) subscription;
        assertThat(disposable.isDisposed())
                .as("AI 응답 대기 중에는 아직 dispose되지 않아야 함")
                .isFalse();

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(disposable.isDisposed())
                .as("연결 종료 시 진행 중인 AI 호출 구독이 dispose되어야 함 (FR-009)")
                .isTrue();
    }
}
