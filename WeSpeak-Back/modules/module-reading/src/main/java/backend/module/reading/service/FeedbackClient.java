package backend.module.reading.service;

import backend.core.grpc.ai.v1.FeedbackChunk;
import backend.core.grpc.ai.v1.FeedbackMetadata;
import backend.core.grpc.ai.v1.FeedbackUploadChunk;
import backend.core.grpc.ai.v1.ReactorFeedbackServiceGrpc;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FeedbackClient {

    private static final int AUDIO_CHUNK_SIZE = 64 * 1024;

    private final ReactorFeedbackServiceGrpc.ReactorFeedbackServiceStub feedbackServiceStub;

    public Flux<FeedbackChunk> feedback(String bookContent, byte[] audioBytes) {
        FeedbackUploadChunk metadataChunk = FeedbackUploadChunk.newBuilder()
                .setMetadata(FeedbackMetadata.newBuilder().setBookContent(bookContent).build())
                .build();

        Flux<FeedbackUploadChunk> audioChunks = Flux.fromIterable(splitIntoChunks(audioBytes))
                .map(chunk -> FeedbackUploadChunk.newBuilder().setAudioChunk(chunk).build());

        Flux<FeedbackUploadChunk> upload = Flux.concat(Flux.just(metadataChunk), audioChunks);

        return feedbackServiceStub
                .withDeadlineAfter(50, TimeUnit.SECONDS)
                .feedback(upload);
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
