package backend.module.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnMissingClass("backend.api.ApiMainApplication")
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
