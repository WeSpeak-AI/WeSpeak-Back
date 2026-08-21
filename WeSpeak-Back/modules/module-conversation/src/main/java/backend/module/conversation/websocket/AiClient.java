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

    //Todo: gRPC 전환 (오디오/응답 스트리밍 재설계, 최우선)
    //Todo: .subscribe()로 논블로킹 전환 (WS 세션 스레드 점유 중, 최우선)
    public AiChatResponse chat(byte[] audioBytes, List<Map<String, String>> history) {
        String historyJson = DataSerializer.serialize(history);

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() { return "audio.m4a"; }
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
