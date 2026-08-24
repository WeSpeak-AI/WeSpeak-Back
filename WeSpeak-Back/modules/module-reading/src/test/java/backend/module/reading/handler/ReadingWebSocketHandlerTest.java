package backend.module.reading.handler;

import backend.module.reading.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * spec FR-009(클라이언트 연결 종료 시 진행 중인 AI 호출 취소) 회귀 검증.
 * AI 서버가 응답하지 않는 상황을 {@link Mono#never()}로 흉내내, WebSocket 세션이 닫히면
 * 진행 중인 구독이 실제로 dispose()되는지 확인한다. Docker/AI 서버 없이 결정적으로 검증 가능.
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
        when(readingService.processUserSummary(any(), anyLong(), any())).thenReturn(Mono.never());

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
}
