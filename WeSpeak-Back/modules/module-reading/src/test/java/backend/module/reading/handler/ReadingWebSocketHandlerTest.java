package backend.module.reading.handler;

import backend.core.common.dataserializer.DataSerializer;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.grpc.ai.v1.FeedbackChunk;
import backend.module.reading.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * spec 004(gRPC 스트리밍) 회귀 검증. {@link FeedbackChunk}는 user_text_final/feedback_text_delta/
 * stream_error만 가지며(오디오 응답 없음, feedback_service.py에 TTS 미적용), Chat 트랙과 동일한
 * JSON 봉투({"type": ...})로 프론트에 전달된다.
 * - disposesInFlightAiCallWhenConnectionCloses: FR-009(연결 종료 시 진행 중인 AI 호출 취소).
 *   {@link Flux#never()}로 AI 서버가 응답하지 않는 상황을 흉내내, 세션이 닫히면 구독이 실제로
 *   dispose()되는지 확인한다.
 * - sendsProgressiveChunksAndDoneOnCompletion: FR-003(청크 단위 점진적 전달). 여러 청크로 나뉘어
 *   오는 텍스트가 타입별로 올바르게 전송되고, 스트림 종료 시 'done' 신호가 마지막으로 전송되는지
 *   확인한다.
 * - sendsErrorSignalWhenTicketConsumptionFailsSynchronously: 실 인프라 라이브 검증(spec 004 T026,
 *   2026-08-25) 중 발견한 버그의 회귀 테스트. {@code consumeTicket}이 Flux 체인 밖에서 동기적으로
 *   {@link BusinessException}을 던지면(예: 티켓 소진) {@code .subscribe()}의 에러 콜백은 전혀 호출되지
 *   않아, 이를 잡아주지 않으면 세션이 아무 에러 메시지 없이 조용히 끊긴다 — try/catch로 잡아 기존
 *   FR-009 에러 처리 경로(handleFeedbackFailure)로 보내는지 확인한다.
 * 셋 다 Docker/AI 서버 없이 결정적으로 검증 가능.
 */
@ExtendWith(MockitoExtension.class)
class ReadingWebSocketHandlerTest {

    @Mock
    private ReadingService readingService;
    @Mock
    private WebSocketSession session;

    @Test
    void disposesInFlightAiCallWhenConnectionCloses() throws Exception {
        ReadingWebSocketHandler handler = new ReadingWebSocketHandler(readingService);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{1, 2, 3});

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("audioBuffer", buffer);
        attributes.put("email", "user@test.com");

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/reading/1"));
        when(readingService.processUserSummary(any(), anyLong(), any())).thenReturn(Flux.never());

        handler.handleTextMessage(session, new TextMessage("END"));

        Object subscription = attributes.get("activeFeedbackSubscription");
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
    void sendsProgressiveChunksAndDoneOnCompletion() throws Exception {
        ReadingWebSocketHandler handler = new ReadingWebSocketHandler(readingService);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{1, 2, 3});

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("audioBuffer", buffer);
        attributes.put("email", "user@test.com");

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/reading/2"));

        FeedbackChunk userTextChunk = FeedbackChunk.newBuilder().setUserTextFinal("summary").build();
        FeedbackChunk delta1 = FeedbackChunk.newBuilder().setFeedbackTextDelta("Great ").build();
        FeedbackChunk delta2 = FeedbackChunk.newBuilder().setFeedbackTextDelta("job!").build();

        when(readingService.processUserSummary(any(), anyLong(), any()))
                .thenReturn(Flux.just(userTextChunk, delta1, delta2));

        handler.handleTextMessage(session, new TextMessage("END"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        List<TextMessage> sent = captor.getAllValues();

        Map<String, Object> first = DataSerializer.deserialize(sent.get(0).getPayload(), Map.class);
        assertThat(first.get("type")).isEqualTo("user_text");
        assertThat(first.get("text")).isEqualTo("summary");

        Map<String, Object> lastMessage = DataSerializer.deserialize(sent.get(sent.size() - 1).getPayload(), Map.class);
        assertThat(lastMessage.get("type"))
                .as("스트림 종료 시 'done' 신호가 마지막으로 전송되어야 함")
                .isEqualTo("done");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsErrorSignalWhenTicketConsumptionFailsSynchronously() throws Exception {
        ReadingWebSocketHandler handler = new ReadingWebSocketHandler(readingService);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{1, 2, 3});

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("audioBuffer", buffer);
        attributes.put("email", "user@test.com");

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/reading/3"));
        when(readingService.processUserSummary(any(), anyLong(), any()))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_TICKET));

        handler.handleTextMessage(session, new TextMessage("END"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        Map<String, Object> errorMessage = DataSerializer.deserialize(captor.getValue().getPayload(), Map.class);
        assertThat(errorMessage.get("type"))
                .as("동기적으로 발생한 BusinessException도 에러 신호로 전달되어야 함")
                .isEqualTo("error");
        assertThat(errorMessage.get("message")).isEqualTo(ErrorCode.INSUFFICIENT_TICKET.getMessage());

        verify(session).close(CloseStatus.SERVER_ERROR);
    }
}
