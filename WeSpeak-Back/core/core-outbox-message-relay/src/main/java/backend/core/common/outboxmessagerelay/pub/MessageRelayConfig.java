package backend.cowrite.common.outboxmessagerelay.pub;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
@ComponentScan("backend.cowrite.common.outboxmessagerelay")
@EnableAsync
public class MessageRelayConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, String> messageRelayKafkaTemplate() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // ip port 설정
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); //메시지 "키"를 String -> byte[] 로 직렬화
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 메시지 "값"을 String -> byte[] 로 직렬화
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); //  acks 설정: "all"이면 리더 + 모든 ISR 복제본까지 기록되어야 성공(가장 안전, 지연/처리량 감소 가능)
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
    }

    @Bean
    public Executor messageRelayPublishEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // 코어 스레드 수(상시 20개 유지).
        executor.setMaxPoolSize(50); //최대  스레드 수. (코어 스레드&큐가 꽉 찼을 때 최대 50개까지 늘림).
        executor.setQueueCapacity(100); // 큐의 개수, 코어 스레드가 바쁠 때 큐에서 대기, 큐와 스레드 둘 다 꽉 차면 예외 발생
        executor.setThreadNamePrefix("mr-pub-event-"); // 생성되는 스레드 이름 접두사(로그 추적에 유용).
        return executor;
    }

    @Bean
    public Executor messageRelayPublishPendingEventExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
