package backend.module.conversation.websocket;

import backend.core.common.dataserializer.DataSerializer;
import backend.core.grpc.ai.v1.ChatChunk;
import backend.module.conversation.domain.Conversation;
import backend.module.conversation.repository.ConversationMessageRepository;
import backend.module.conversation.service.ConversationMessageService;
import backend.module.conversation.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * spec 004(gRPC 스트리밍) 회귀 검증. AI 서버는 TTS를 만들지 않고 텍스트만 스트리밍하며(오디오는
 * 프론트 내장 TTS가 담당), {@link ChatChunk}는 user_text_final/ai_text_delta/stream_error만 갖는다.
 * - disposesInFlightAiCallWhenConnectionCloses: FR-009(연결 종료 시 진행 중인 AI 호출 취소).
 *   {@link Flux#never()}로 AI 서버가 응답하지 않는 상황을 흉내내, 세션이 닫히면 구독이 실제로
 *   dispose()되는지 확인한다.
 * - aggregatesChunksAndPersistsOnCompletion: FR-006(스트림 종료 시 누적 콘텐츠가 기존과 동일).
 *   여러 청크로 나뉘어 오는 텍스트가 정확히 합쳐져서 DB에 1회 저장되고, 스트림 종료 시 'done'
 *   신호가 전송되는지 확인한다.
 * 둘 다 Docker/Kafka/AI 서버 없이 결정적으로 검증 가능.
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
        when(aiClient.chat(any(), any())).thenReturn(Flux.never());

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

    @Test
    @SuppressWarnings("unchecked")
    void aggregatesChunksAndPersistsOnCompletion() throws Exception {
        ConversationWebSocketHandler handler = new ConversationWebSocketHandler(
                redisTemplate, conversationMessageService, conversationMessageRepository,
                conversationService, aiClient);

        Conversation conversation = Conversation.builder()
                .conversationId(2L)
                .userEmail("user@test.com")
                .build();

        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/conversation/2"));
        when(conversationService.getConversation("2")).thenReturn(conversation);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(conversationMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(List.of());

        ChatChunk userTextChunk = ChatChunk.newBuilder().setUserTextFinal("hello").build();
        ChatChunk aiDelta1 = ChatChunk.newBuilder().setAiTextDelta("Hi ").build();
        ChatChunk aiDelta2 = ChatChunk.newBuilder().setAiTextDelta("there!").build();

        when(aiClient.chat(any(), any()))
                .thenReturn(Flux.just(userTextChunk, aiDelta1, aiDelta2));

        handler.handleBinaryMessage(session, new BinaryMessage(ByteBuffer.wrap(new byte[]{1, 2, 3})));

        verify(conversationMessageService).saveUserMessageToDB(conversation, "hello");
        verify(conversationMessageService).saveAiMessageToDB(conversation, "Hi there!");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        List<TextMessage> sent = captor.getAllValues();
        Map<String, Object> lastMessage = DataSerializer.deserialize(sent.get(sent.size() - 1).getPayload(), Map.class);
        assertThat(lastMessage.get("type"))
                .as("스트림 종료 시 'done' 신호가 마지막으로 전송되어야 함")
                .isEqualTo("done");
    }
}
