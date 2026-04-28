package backend.core.webclient.stt;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ExternalSttService implements SttService {

    private final WebClient sttWebClient;

    public ExternalSttService(@Qualifier("sttWebClient") WebClient sttWebClient) {
        this.sttWebClient = sttWebClient;
    }

    @Override
    public String transcribe(byte[] audioBytes) {
        return sttWebClient.post()
                .uri("/stt/transcribe")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(audioBytes)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
