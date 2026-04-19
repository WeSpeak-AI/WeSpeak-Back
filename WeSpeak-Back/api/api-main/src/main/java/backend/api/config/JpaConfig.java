package backend.api.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = "backend.core.domain")
@EnableJpaRepositories(basePackages = {"backend.module", "backend.core.infra.repository"})
public class JpaConfig {
}
