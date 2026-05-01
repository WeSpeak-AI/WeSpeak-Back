package backend.module.conversation.websocket;

import backend.core.common.dataserializer.DataSerializer;
import backend.module.conversation.dto.AiChatResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AiClient {

    private RestClient restClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @PostConstruct
    public void init() {
        restClient = RestClient.create(aiServerUrl);
    }

    public AiChatResponse chat(byte[] audioBytes, List<Map<String, String>> history) {
        String historyJson = DataSerializer.serialize(history);

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() { return "audio.wav"; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioResource);
        body.add("history", historyJson);

        return restClient.post()
                .uri("/chat")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AiChatResponse.class);
    }
}
