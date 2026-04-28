package backend.module.reading.config;

import backend.module.reading.handler.ReadingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ReadingWebSocketConfig implements WebSocketConfigurer {

    private final ReadingWebSocketHandler readingWebSocketHandler;
    private final ReadingHandshakeInterceptor readingHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(readingWebSocketHandler, "/ws/reading/*")
                .addInterceptors(readingHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
