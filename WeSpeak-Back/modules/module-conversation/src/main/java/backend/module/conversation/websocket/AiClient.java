package backend.module.conversation.websocket;

import backend.core.common.dataserializer.DataSerializer;
import backend.module.conversation.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiClient {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;

    public AiChatResponse chat(byte[] audioBytes, List<Map<String, String>> history) {
        String historyJson = DataSerializer.serialize(history);

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() { return "audio.wav"; }
        };

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", audioResource);
        bodyBuilder.part("history", historyJson);

        return aiWebClient.post()
                .uri("/chat")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(AiChatResponse.class)
                .timeout(Duration.ofSeconds(60))
                .block();
    }
}
