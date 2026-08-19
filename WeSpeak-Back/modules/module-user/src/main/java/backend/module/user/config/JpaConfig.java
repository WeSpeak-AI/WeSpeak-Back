package backend.module.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ConditionalOnMissingClass("backend.api.ApiMainApplication")
@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"backend.core.common.outboxmessagerelay", "backend.module.user"})
@EnableJpaRepositories(basePackages = {"backend.module.user", "backend.core.common.outboxmessagerelay"})
public class JpaConfig {
}
