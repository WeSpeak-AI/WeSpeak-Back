package backend.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Auth auth = new Auth();

    @Getter
    @Setter
    public static class Auth {
        private String tokenSecret;
        private long tokenExpirationMsec;
    }
}
