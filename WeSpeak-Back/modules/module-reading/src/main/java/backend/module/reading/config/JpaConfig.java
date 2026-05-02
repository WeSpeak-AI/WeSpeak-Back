package backend.module.reading.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ConditionalOnMissingClass("backend.api.ApiMainApplication")
@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"backend.core.domain", "backend.core.common.outboxmessagerelay"})
@EnableJpaRepositories(basePackages = {"backend.module.reading", "backend.core.infra.repository", "backend.core.common.outboxmessagerelay"})
public class JpaConfig {
}
