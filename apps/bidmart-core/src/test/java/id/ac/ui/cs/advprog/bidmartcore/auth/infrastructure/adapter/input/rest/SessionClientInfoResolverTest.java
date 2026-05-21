package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class SessionClientInfoResolverTest {

    private final SessionClientInfoResolver resolver = new SessionClientInfoResolver();

    @Test
    void resolve_withUserAgentAndForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Chrome/120");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 70.41.3.18");
        request.addHeader("X-Geo-Country", "US");

        var info = resolver.resolve(request);

        assertEquals("Chrome/120", info.deviceInfo());
        assertEquals("203.0.113.5", info.ipAddress());
        assertEquals("US", info.locationLabel());
    }

    @Test
    void resolve_withDeviceNameOverride() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Chrome");
        request.addHeader("X-Device-Name", "My Laptop");
        request.setRemoteAddr("192.168.1.1");

        var info = resolver.resolve(request);

        assertEquals("My Laptop", info.deviceInfo());
    }

    @Test
    void resolve_withRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.1");
        request.setRemoteAddr("192.168.1.1");

        var info = resolver.resolve(request);

        assertEquals("10.0.0.1", info.ipAddress());
    }

    @Test
    void resolve_fallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");

        var info = resolver.resolve(request);

        assertEquals("192.168.1.1", info.ipAddress());
    }

    @Test
    void resolve_withCfCountry() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-IPCountry", "ID");
        request.setRemoteAddr("127.0.0.1");

        var info = resolver.resolve(request);

        assertEquals("ID", info.locationLabel());
    }

    @Test
    void resolve_withCountryCode() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Country-Code", "SG");
        request.setRemoteAddr("127.0.0.1");

        var info = resolver.resolve(request);

        assertEquals("SG", info.locationLabel());
    }

    @Test
    void resolve_withLocationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Location", "JP");
        request.setRemoteAddr("127.0.0.1");

        var info = resolver.resolve(request);

        assertEquals("JP", info.locationLabel());
    }

    @Test
    void resolve_noHeaders_shouldUseDefaults() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        var info = resolver.resolve(request);

        assertNotNull(info);
        assertNotNull(info.deviceInfo());
        assertNotNull(info.ipAddress());
        assertNotNull(info.locationLabel());
    }
}
