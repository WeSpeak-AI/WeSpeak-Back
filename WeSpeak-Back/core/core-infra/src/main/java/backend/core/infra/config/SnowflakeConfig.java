package backend.core.infra.config;

import backend.core.infra.Snowflake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Bean
    public Snowflake snowflake(@Value("${NODE_ID:0}") long nodeId) {
        return new Snowflake(nodeId);
    }
}
