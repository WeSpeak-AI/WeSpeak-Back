package backend.core.webclient.tts;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Map;

@Service
public class ExternalTtsService implements TtsService {

    private final WebClient aiWebClient;

    public ExternalTtsService(@Qualifier("aiWebClient") WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    @Override
    public String generateUrl(String text) {
        return aiWebClient.post()
                .uri("/tts/url")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    @Override
    public void stream(String text, OutputStream outputStream) throws IOException {
        byte[] audioBytes = aiWebClient.post()
                .uri("/tts/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(30))
                .block();
        if (audioBytes != null) {
            outputStream.write(audioBytes);
        }
    }
}
