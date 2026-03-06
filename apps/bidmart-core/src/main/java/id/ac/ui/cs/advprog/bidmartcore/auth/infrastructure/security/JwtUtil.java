package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-valid-ms}")
    private long refreshValidMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtToken generateAccessToken(String sessionId) {
        Instant expirationTime = Instant.ofEpochMilli(System.currentTimeMillis() + expirationMs);
        String tokenString = Jwts.builder()
                .subject(sessionId)
                .issuedAt(new Date())
                .expiration(Date.from(expirationTime))
                .signWith(getSigningKey())
                .compact();

        JwtToken token = new JwtToken();
        token.setToken(tokenString);
        token.setExpirationTime(expirationTime);

        return token;
    }

    public JwtToken generateRefreshToken(String sessionId) {
        Instant expirationTime = Instant.ofEpochMilli(System.currentTimeMillis() + refreshValidMs);
        String tokenString =  Jwts.builder()
                .subject(sessionId)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(Date.from(expirationTime))
                .signWith(getSigningKey())
                .compact();

        JwtToken token = new JwtToken();
        token.setToken(tokenString);
        token.setExpirationTime(expirationTime);

        return token;
    }

    public String extractSessionId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean isRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return "refresh".equals(claims.get("type", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
