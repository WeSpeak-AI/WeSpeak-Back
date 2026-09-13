package backend.module.writing.config;

import backend.core.common.exception.BusinessException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@ConditionalOnMissingClass("backend.api.ApiMainApplication")
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${writing.ai-correction.retry.max-retries:6}")
    private int aiCorrectionMaxRetries;

    @Value("${writing.ai-correction.retry.initial-interval-ms:1000}")
    private long aiCorrectionInitialIntervalMs;

    @Value("${writing.ai-correction.retry.max-interval-ms:30000}")
    private long aiCorrectionMaxIntervalMs;

    @Bean
    public DefaultKafkaConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaTemplate<String, String> messageRelayKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(messageRelayKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> aiCorrectionKafkaListenerContainerFactory(
            KafkaTemplate<String, String> messageRelayKafkaTemplate) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 900000); // 15분

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(messageRelayKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, aiCorrectionBackOff());
        // 에세이 삭제(ESSAY_NOT_FOUND)·핸들러 없음처럼 다시 시도해도 결과가 같은 실패는 즉시 DLT로 보낸다.
        errorHandler.addNotRetryableExceptions(BusinessException.class, IllegalArgumentException.class);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * AI 서버의 짧은 장애를 흡수하기 위한 지수 백오프. 기본값 기준 1s → 2s → 4s → 8s → 16s → 30s(총 약 1분).
     * 재시도 대기 동안 파티션이 멈추지만, AI 서버 장애 중에는 다음 레코드도 실패하므로 서버를 계속 두드리지 않는 효과가 있다.
     * 1회 시도(AI timeout 5분) + 최대 대기 간격이 max.poll.interval.ms(15분)보다 짧아야 한다.
     */
    private ExponentialBackOffWithMaxRetries aiCorrectionBackOff() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(aiCorrectionMaxRetries);
        backOff.setInitialInterval(aiCorrectionInitialIntervalMs);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(aiCorrectionMaxIntervalMs);
        return backOff;
    }

    // writing.DLT 소비용. DLT 처리 자체가 실패하면 짧게 재시도 후 로그만 남기고 넘어간다(DLT의 DLT는 두지 않음).
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> aiCorrectionDltKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2L)));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public NewTopic writingDlt() {
        return TopicBuilder.name("writing.DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic topicDlt() {
        return TopicBuilder.name("topic.DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic userDeletedDlt() {
        return TopicBuilder.name("user-deleted.DLT").partitions(1).replicas(1).build();
    }
}
