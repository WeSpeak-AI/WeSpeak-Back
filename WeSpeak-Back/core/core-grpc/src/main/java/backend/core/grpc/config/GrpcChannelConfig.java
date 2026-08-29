package backend.core.grpc.config;

import backend.core.grpc.ai.v1.ReactorChatServiceGrpc;
import backend.core.grpc.ai.v1.ReactorFeedbackServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcChannelConfig {

    @Value("${ai.server.grpc-host:wespeak-ai}")
    private String aiServerGrpcHost;

    @Value("${ai.server.grpc-port:50051}")
    private int aiServerGrpcPort;

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel aiGrpcChannel() {
        return ManagedChannelBuilder.forAddress(aiServerGrpcHost, aiServerGrpcPort)
                .usePlaintext()
                .build();
    }

    @Bean
    public ReactorChatServiceGrpc.ReactorChatServiceStub chatServiceStub(ManagedChannel aiGrpcChannel) {
        return ReactorChatServiceGrpc.newReactorStub(aiGrpcChannel);
    }

    @Bean
    public ReactorFeedbackServiceGrpc.ReactorFeedbackServiceStub feedbackServiceStub(ManagedChannel aiGrpcChannel) {
        return ReactorFeedbackServiceGrpc.newReactorStub(aiGrpcChannel);
    }
}
