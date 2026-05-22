package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AuthCookieServiceTest {

    private AuthCookieService service;

    @BeforeEach
    void setUp() {
        service = new AuthCookieService();
        ReflectionTestUtils.setField(service, "accessTokenCookieName", "access");
        ReflectionTestUtils.setField(service, "refreshTokenCookieName", "refresh");
        ReflectionTestUtils.setField(service, "cookiePath", "/");
        ReflectionTestUtils.setField(service, "cookieDomain", "");
        ReflectionTestUtils.setField(service, "sameSite", "Lax");
        ReflectionTestUtils.setField(service, "secure", false);
        ReflectionTestUtils.setField(service, "accessTokenTtlMs", 1000L);
        ReflectionTestUtils.setField(service, "refreshTokenTtlMs", 2000L);
    }

    @Test
    void buildAuthCookies_shouldIncludeTwoCookies() {
        var cookies = service.buildAuthCookies("access-token", "refresh-token");

        assertEquals(2, cookies.size());
        assertTrue(cookies.get(0).contains("access=access-token"));
        assertTrue(cookies.get(1).contains("refresh=refresh-token"));
    }

    @Test
    void clearAuthCookies_shouldHaveZeroMaxAge() {
        var cookies = service.clearAuthCookies();

        assertTrue(cookies.get(0).contains("Max-Age=0"));
        assertTrue(cookies.get(1).contains("Max-Age=0"));
    }

    @Test
    void resolveAccessToken_whenCookieMissing_shouldEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertTrue(service.resolveAccessToken(request).isEmpty());
    }

    @Test
    void resolveAccessToken_whenCookiePresent_shouldReturnValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access", "token"));

        assertEquals("token", service.resolveAccessToken(request).orElse(null));
    }

    @Test
    void resolveRefreshToken_whenCookiePresent_shouldReturnValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh", "refresh-token"));

        assertEquals("refresh-token", service.resolveRefreshToken(request).orElse(null));
    }

    @Test
    void resolveAccessToken_whenCookieEmptyValue_shouldEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access", ""));

        assertTrue(service.resolveAccessToken(request).isEmpty());
    }

    @Test
    void buildAuthCookies_withDomain_shouldIncludeDomain() {
        ReflectionTestUtils.setField(service, "cookieDomain", ".example.com");
        var cookies = service.buildAuthCookies("at", "rt");

        assertTrue(cookies.get(0).contains("Domain=.example.com"));
    }

    @Test
    void buildAuthCookies_secure_shouldIncludeSecure() {
        ReflectionTestUtils.setField(service, "secure", true);
        var cookies = service.buildAuthCookies("at", "rt");

        assertTrue(cookies.get(0).contains("Secure"));
    }
}
