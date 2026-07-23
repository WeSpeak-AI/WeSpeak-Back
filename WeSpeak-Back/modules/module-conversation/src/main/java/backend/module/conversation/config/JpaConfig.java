package backend.module.conversation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ConditionalOnMissingClass("backend.api.ApiMainApplication")
@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"backend.core.common.outboxmessagerelay", "backend.module.conversation"})
@EnableJpaRepositories(basePackages = {"backend.module.conversation", "backend.core.common.outboxmessagerelay"})
public class JpaConfig {
}
