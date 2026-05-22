package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshValidMs", 300000L);
    }

    @Test
    void generateAccessToken_shouldBeValid() {
        JwtToken token = jwtUtil.generateAccessToken("session-id");

        assertNotNull(token.getToken());
        assertTrue(jwtUtil.isTokenValid(token.getToken()));
        assertFalse(jwtUtil.isRefreshToken(token.getToken()));
        assertEquals("session-id", jwtUtil.extractSessionId(token.getToken()));
    }

    @Test
    void generateRefreshToken_shouldBeRefresh() {
        JwtToken token = jwtUtil.generateRefreshToken("session-id");

        assertTrue(jwtUtil.isRefreshToken(token.getToken()));
        assertEquals("session-id", jwtUtil.extractSessionId(token.getToken()));
    }

    @Test
    void emailVerificationToken_shouldValidate() {
        UUID userId = UUID.randomUUID();
        JwtToken token = jwtUtil.generateEmailVerificationToken(userId, "user@example.com", 60L);

        jwtUtil.validateEmailVerificationToken(token.getToken(), userId, "user@example.com");
    }

    @Test
    void validateEmailVerificationToken_whenMismatch_shouldThrow() {
        UUID userId = UUID.randomUUID();
        JwtToken token = jwtUtil.generateEmailVerificationToken(userId, "user@example.com", 60L);

        assertThrows(IllegalArgumentException.class,
                () -> jwtUtil.validateEmailVerificationToken(token.getToken(), userId, "other@example.com"));
    }

    @Test
    void isTokenValid_whenExpired_shouldReturnFalse() {
        JwtToken token = jwtUtil.generateAccessToken("session-id");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        JwtToken expired = jwtUtil.generateAccessToken("session-id");

        assertTrue(jwtUtil.isTokenValid(token.getToken()));
        assertFalse(jwtUtil.isTokenValid(expired.getToken()));
    }
}

