package backend.gateway.filter;

import backend.core.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 모든 요청에 적용되는 JWT 인증 필터.
 *
 * 처리 흐름:
 * 1. PUBLIC_PATHS 에 속하면 JWT 검증 없이 통과
 * 2. Authorization 헤더에서 Bearer 토큰 추출
 * 3. 서명/만료 검증 실패 시 401 반환
 * 4. 검증 성공 시 X-Username 헤더를 추가해 downstream 서비스로 전달
 *    → 각 서비스는 이 헤더를 신뢰하고 @RequestHeader("X-Username") 또는
 *      커스텀 ArgumentResolver로 사용자 정보를 가져옵니다
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/isDuplicate",
            "/api/auth/google",
            "/api/oauth2"
    );
    private static final String ADMIN_PATH_PREFIX = "/api/admin";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest().getHeaders());

        boolean valid;
        try {
            valid = token != null && jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            valid = false;
        }

        if (!valid) {
            log.warn("JWT validation failed for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            byte[] body = "{\"success\":false,\"code\":\"A004\",\"message\":\"인증이 필요합니다.\"}".getBytes();
            return exchange.getResponse().writeWith(
                    reactor.core.publisher.Mono.just(exchange.getResponse().bufferFactory().wrap(body))
            );
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);

        if (path.startsWith(ADMIN_PATH_PREFIX) && !"ADMIN".equals(role)) {
            log.warn("Access denied for path: {}, role: {}", path, role);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-Username", username)
                        .header("X-Role", role)
                        .build())
                .build();

        return chain.filter(mutatedExchange);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(HttpHeaders headers) {
        String bearerToken = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -1; // 다른 필터보다 먼저 실행
    }
}
