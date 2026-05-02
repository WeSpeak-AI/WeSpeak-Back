package backend.core.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider{

    private final long expirationMs;
    private final SecretKey secretKey;

    public JwtTokenProvider(
            @Value("${app.auth.token-secret}") String secret,
            @Value("${app.auth.token-expiration-msec}") long expirationMs
    ) {
        this.expirationMs = expirationMs;
        this.secretKey = buildSigningKey(secret);
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    private SecretKey buildSigningKey(String secret) {
        byte[] keyBytes = Decoders.BASE64URL.decode(secret);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 64 bytes (512 bits) for HS512");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Jwts.parser()
                    .verifyWith(secretKey)   // SecretKey 객체 (예: Keys.hmacShaKeyFor(...))
                    .build()
                    .parseSignedClaims(token); // 유효성/서명 검증 수행

            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature/token");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT token is empty or invalid.");
        }
        return false;
    }
}
