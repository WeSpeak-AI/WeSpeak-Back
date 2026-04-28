package backend.core.webclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${tts.server.url}")
    private String ttsServerUrl;

    @Value("${stt.server.url}")
    private String sttServerUrl;

    @Bean
    public WebClient aiWebClient() {
        return WebClient.create(aiServerUrl);
    }

    @Bean
    public WebClient ttsWebClient() {
        return WebClient.create(ttsServerUrl);
    }

    @Bean
    public WebClient sttWebClient() {
        return WebClient.create(sttServerUrl);
    }
}
