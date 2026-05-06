package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class AuthCookieService {

    @Value("${auth.cookie.access-token-name:bm_access_token}")
    private String accessTokenCookieName;

    @Value("${auth.cookie.refresh-token-name:bm_refresh_token}")
    private String refreshTokenCookieName;

    @Value("${auth.cookie.path:/}")
    private String cookiePath;

    @Value("${auth.cookie.domain:}")
    private String cookieDomain;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${auth.cookie.secure:false}")
    private boolean secure;

    @Value("${jwt.expiration-ms}")
    private long accessTokenTtlMs;

    @Value("${jwt.refresh-valid-ms}")
    private long refreshTokenTtlMs;

    public List<String> buildAuthCookies(String accessToken, String refreshToken) {
        return List.of(
                buildCookie(accessTokenCookieName, accessToken, accessTokenTtlMs),
                buildCookie(refreshTokenCookieName, refreshToken, refreshTokenTtlMs)
        );
    }

    public List<String> clearAuthCookies() {
        return List.of(
                buildCookie(accessTokenCookieName, "", 0),
                buildCookie(refreshTokenCookieName, "", 0)
        );
    }

    public Optional<String> resolveAccessToken(HttpServletRequest request) {
        return resolveCookie(request, accessTokenCookieName);
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        return resolveCookie(request, refreshTokenCookieName);
    }

    private Optional<String> resolveCookie(HttpServletRequest request, String cookieName) {
        var cookie = WebUtils.getCookie(request, cookieName);
        if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
            return Optional.empty();
        }
        return Optional.of(cookie.getValue());
    }

    private String buildCookie(String name, String value, long maxAgeMs) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(Duration.ofMillis(maxAgeMs));

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }

        return builder.build().toString();
    }
}

