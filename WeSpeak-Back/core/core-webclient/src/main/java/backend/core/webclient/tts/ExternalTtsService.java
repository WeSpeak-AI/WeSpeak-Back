package backend.core.webclient.tts;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Service
public class ExternalTtsService implements TtsService {

    private final WebClient ttsWebClient;

    public ExternalTtsService(@Qualifier("ttsWebClient") WebClient ttsWebClient) {
        this.ttsWebClient = ttsWebClient;
    }

    @Override
    public String generateUrl(String text) {
        return ttsWebClient.post()
                .uri("/tts/url")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public void stream(String text, OutputStream outputStream) throws IOException {
        byte[] audioBytes = ttsWebClient.post()
                .uri("/tts/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
        if (audioBytes != null) {
            outputStream.write(audioBytes);
        }
    }
}
