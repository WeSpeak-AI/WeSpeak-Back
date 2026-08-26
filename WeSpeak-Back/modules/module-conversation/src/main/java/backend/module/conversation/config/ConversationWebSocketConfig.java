package backend.module.conversation.config;

import backend.module.conversation.websocket.ConversationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class ConversationWebSocketConfig implements WebSocketConfigurer {

    private final ConversationWebSocketHandler conversationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(conversationWebSocketHandler, "/ws/conversation/*")
                .setAllowedOrigins("*");
    }

    /**
     * ConversationScreen.tsx는 한 발화의 오디오 전체를 ws.send() 1회로 보낸다(청크 분할 없음).
     * 내장 Tomcat WebSocket 컨테이너의 기본 바이너리 메시지 버퍼(8KB)는 짧은 발화도 초과하기
     * 쉬워 연결이 끊기므로(1009 message too big), 발화 길이를 감안해 버퍼 크기를 키운다.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        return container;
    }
}
