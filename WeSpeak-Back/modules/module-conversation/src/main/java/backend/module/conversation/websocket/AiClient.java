package backend.module.conversation.websocket;

import backend.core.common.dataserializer.DataSerializer;
import backend.core.grpc.ai.v1.ChatChunk;
import backend.core.grpc.ai.v1.ChatMetadata;
import backend.core.grpc.ai.v1.ChatUploadChunk;
import backend.core.grpc.ai.v1.ReactorChatServiceGrpc;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AiClient {

    private static final int AUDIO_CHUNK_SIZE = 64 * 1024;

    private final ReactorChatServiceGrpc.ReactorChatServiceStub chatServiceStub;

    public Flux<ChatChunk> chat(byte[] audioBytes, List<Map<String, String>> history) {
        String historyJson = DataSerializer.serialize(history);

        ChatUploadChunk metadataChunk = ChatUploadChunk.newBuilder()
                .setMetadata(ChatMetadata.newBuilder().setHistoryJson(historyJson).build())
                .build();

        Flux<ChatUploadChunk> audioChunks = Flux.fromIterable(splitIntoChunks(audioBytes))
                .map(chunk -> ChatUploadChunk.newBuilder().setAudioChunk(chunk).build());

        Flux<ChatUploadChunk> upload = Flux.concat(Flux.just(metadataChunk), audioChunks);

        return chatServiceStub
                .withDeadlineAfter(60, TimeUnit.SECONDS)
                .chat(upload);
    }

    private List<ByteString> splitIntoChunks(byte[] audioBytes) {
        List<ByteString> chunks = new ArrayList<>();
        for (int offset = 0; offset < audioBytes.length; offset += AUDIO_CHUNK_SIZE) {
            int end = Math.min(offset + AUDIO_CHUNK_SIZE, audioBytes.length);
            chunks.add(ByteString.copyFrom(audioBytes, offset, end - offset));
        }
        if (chunks.isEmpty()) {
            chunks.add(ByteString.EMPTY);
        }
        return chunks;
    }
}
