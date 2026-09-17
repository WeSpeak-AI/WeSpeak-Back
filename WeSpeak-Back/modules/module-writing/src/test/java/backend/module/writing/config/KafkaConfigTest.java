package backend.module.writing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaConfigTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> template = mock(KafkaTemplate.class);

    private KafkaConfig config(int concurrency, int partitions) {
        KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "test-group");
        ReflectionTestUtils.setField(config, "aiCorrectionMaxRetries", 6);
        ReflectionTestUtils.setField(config, "aiCorrectionInitialIntervalMs", 1000L);
        ReflectionTestUtils.setField(config, "aiCorrectionMaxIntervalMs", 30000L);
        ReflectionTestUtils.setField(config, "aiCorrectionConcurrency", concurrency);
        ReflectionTestUtils.setField(config, "aiCorrectionPartitions", partitions);
        return config;
    }

    @Test
    void aiCorrectionConsumerUsesConfiguredConcurrency() {
        ConcurrentMessageListenerContainer<String, String> container =
                config(3, 3).aiCorrectionKafkaListenerContainerFactory(template).createContainer("writing");

        assertThat(container.getConcurrency()).isEqualTo(3);
    }

    @Test
    void writingTopicUsesConfiguredPartitions() {
        NewTopic topic = config(3, 3).writingTopic();

        assertThat(topic.name()).isEqualTo("writing");
        assertThat(topic.numPartitions()).isEqualTo(3);
    }

    @Test
    void singleConsumerAndPartitionCanStillBeConfigured() {
        KafkaConfig config = config(1, 1);

        assertThat(config.aiCorrectionKafkaListenerContainerFactory(template).createContainer("writing").getConcurrency()).isEqualTo(1);
        assertThat(config.writingTopic().numPartitions()).isEqualTo(1);
    }
}
