package backend.module.conversation.websocket;

import backend.core.grpc.ai.v1.ChatChunk;
import backend.core.grpc.ai.v1.ChatUploadChunk;
import backend.core.grpc.ai.v1.ReactorChatServiceGrpc;
import backend.core.grpc.ai.v1.StreamError;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 004 T022 — quickstart.md 1단계: AI 서버 없이 {@code InProcessServerBuilder} 목업으로
 * {@link AiClient}(gRPC 클라이언트)의 청크 수신·조립·오류·취소 동작을 검증한다.
 * Docker/실제 AI 서버 없이 결정적으로 검증 가능.
 */
class AiClientGrpcStreamingTest {

    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    private AiClient startServerAndClient(ReactorChatServiceGrpc.ChatServiceImplBase serviceImpl) throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName).addService(serviceImpl).build().start();
        channel = InProcessChannelBuilder.forName(serverName).build();
        return new AiClient(ReactorChatServiceGrpc.newReactorStub(channel));
    }

    @Test
    void receivesChunksInOrderAndAssemblesIdenticalContent() throws Exception {
        AiClient aiClient = startServerAndClient(new ReactorChatServiceGrpc.ChatServiceImplBase() {
            @Override
            public Flux<ChatChunk> chat(Flux<ChatUploadChunk> request) {
                return request.thenMany(Flux.just(
                        ChatChunk.newBuilder().setUserTextFinal("hello world").build(),
                        ChatChunk.newBuilder().setAiTextDelta("Hi ").build(),
                        ChatChunk.newBuilder().setAiTextDelta("there").build(),
                        ChatChunk.newBuilder().setAiTextDelta("!").build()
                ));
            }
        });

        List<ChatChunk> received = aiClient.chat(new byte[]{1, 2, 3}, List.of())
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(received).hasSize(4);
        assertThat(received.get(0).getUserTextFinal()).isEqualTo("hello world");

        String assembled = received.stream()
                .filter(chunk -> chunk.getPayloadCase() == ChatChunk.PayloadCase.AI_TEXT_DELTA)
                .map(ChatChunk::getAiTextDelta)
                .collect(Collectors.joining());
        assertThat(assembled)
                .as("나뉘어 온 ai_text_delta를 이어붙인 결과가 목업이 보낸 전체 텍스트와 동일해야 함 (spec FR-006)")
                .isEqualTo("Hi there!");
    }

    @Test
    void propagatesStreamErrorChunk() throws Exception {
        AiClient aiClient = startServerAndClient(new ReactorChatServiceGrpc.ChatServiceImplBase() {
            @Override
            public Flux<ChatChunk> chat(Flux<ChatUploadChunk> request) {
                return request.thenMany(Flux.just(
                        ChatChunk.newBuilder()
                                .setStreamError(StreamError.newBuilder().setMessage("AI 서버 오류").build())
                                .build()
                ));
            }
        });

        ChatChunk chunk = aiClient.chat(new byte[]{1}, List.of()).blockFirst(Duration.ofSeconds(5));

        assertThat(chunk.getPayloadCase())
                .as("목업이 보낸 stream_error가 그대로 전달되어야 함 (spec FR-009)")
                .isEqualTo(ChatChunk.PayloadCase.STREAM_ERROR);
        assertThat(chunk.getStreamError().getMessage()).isEqualTo("AI 서버 오류");
    }

    @Test
    void cancellingClientSubscriptionCancelsServerStream() throws Exception {
        CountDownLatch serverCancelled = new CountDownLatch(1);
        AiClient aiClient = startServerAndClient(new ReactorChatServiceGrpc.ChatServiceImplBase() {
            @Override
            public Flux<ChatChunk> chat(Flux<ChatUploadChunk> request) {
                return request.thenMany(Flux.<ChatChunk>never().doOnCancel(serverCancelled::countDown));
            }
        });

        Disposable subscription = aiClient.chat(new byte[]{1}, List.of()).subscribe();
        Thread.sleep(200); // 서버가 응답 스트림을 시작할 시간을 준다
        subscription.dispose();

        assertThat(serverCancelled.await(5, TimeUnit.SECONDS))
                .as("클라이언트가 구독을 취소하면 진행 중인 gRPC 스트림도 취소되어야 함 (spec FR-009)")
                .isTrue();
    }
}
