package backend.module.auth.jwt;

import backend.core.jwt.JwtTokenParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider extends JwtTokenParser {

    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.auth.token-secret}") String secret,
            @Value("${app.auth.token-expiration-msec}") long expirationMs
    ) {
        super(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
