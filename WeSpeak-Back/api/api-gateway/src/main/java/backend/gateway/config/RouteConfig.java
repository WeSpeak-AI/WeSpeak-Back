package backend.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${services.auth.url:http://localhost:9001}")
    private String authServiceUrl;

    @Value("${services.user.url:http://localhost:9002}")
    private String userServiceUrl;

    @Value("${services.voca.url:http://localhost:9003}")
    private String vocaServiceUrl;

    @Value("${services.writing.url:http://localhost:9004}")
    private String writingServiceUrl;

    @Value("${services.reading.url:http://localhost:9005}")
    private String readingServiceUrl;

    @Value("${services.conversation.url:http://localhost:9006}")
    private String conversationServiceUrl;

    @Value("${services.search.url:http://localhost:9007}")
    private String searchServiceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri(authServiceUrl))
                // User
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .uri(userServiceUrl))
                // Voca (admin routes before generic)
                .route("voca-admin", r -> r
                        .path("/api/admin/voca-books/**", "/api/admin/days/**",
                              "/api/admin/words/**", "/api/admin/voca/**")
                        .uri(vocaServiceUrl))
                .route("voca-service", r -> r
                        .path("/api/voca/**", "/api/custom-voca/**", "/api/test/**")
                        .uri(vocaServiceUrl))
                // Writing
                .route("writing-service", r -> r
                        .path("/api/writing/**")
                        .uri(writingServiceUrl))
                // Reading (admin routes before generic)
                .route("reading-admin", r -> r
                        .path("/api/admin/books/**", "/api/admin/pages/**")
                        .uri(readingServiceUrl))
                .route("reading-service", r -> r
                        .path("/api/reading/**", "/ws/reading/**")
                        .uri(readingServiceUrl))
                // Conversation (admin routes before generic)
                .route("conversation-admin", r -> r
                        .path("/api/admin/topics/**")
                        .uri(conversationServiceUrl))
                .route("conversation-service", r -> r
                        .path("/api/conversation/**", "/ws/conversation/**")
                        .uri(conversationServiceUrl))
                // Search
                .route("search-service", r -> r
                        .path("/api/search/**")
                        .uri(searchServiceUrl))
                .build();
    }
}
