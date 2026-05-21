package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionClientInfoTest {

    @Test
    void unknown_shouldReturnDefaults() {
        SessionClientInfo info = SessionClientInfo.unknown();

        assertEquals("Unknown device", info.deviceInfo());
        assertEquals("unknown", info.ipAddress());
        assertEquals("Unknown location", info.locationLabel());
    }

    @Test
    void of_withNullValues_shouldReturnDefaults() {
        SessionClientInfo info = SessionClientInfo.of(null, null, null);

        assertEquals("Unknown device", info.deviceInfo());
        assertEquals("unknown", info.ipAddress());
        assertEquals("Unknown location", info.locationLabel());
    }

    @Test
    void of_withBlankValues_shouldReturnDefaults() {
        SessionClientInfo info = SessionClientInfo.of("  ", "  ", "  ");

        assertEquals("Unknown device", info.deviceInfo());
        assertEquals("unknown", info.ipAddress());
        assertEquals("Unknown location", info.locationLabel());
    }

    @Test
    void of_withCustomValues_shouldReturnTrimmed() {
        SessionClientInfo info = SessionClientInfo.of("  Chrome  ", "  1.2.3.4  ", "  US  ");

        assertEquals("Chrome", info.deviceInfo());
        assertEquals("1.2.3.4", info.ipAddress());
        assertEquals("US", info.locationLabel());
    }

    @Test
    void of_withLongValue_shouldTruncate() {
        String longDevice = "A".repeat(600);
        SessionClientInfo info = SessionClientInfo.of(longDevice, null, null);

        assertEquals(512, info.deviceInfo().length());
    }

    @Test
    void of_longIp_shouldTruncate() {
        String longIp = "A".repeat(100);
        SessionClientInfo info = SessionClientInfo.of(null, longIp, null);

        assertEquals(64, info.ipAddress().length());
    }

    @Test
    void of_longLocation_shouldTruncate() {
        String longLoc = "A".repeat(300);
        SessionClientInfo info = SessionClientInfo.of(null, null, longLoc);

        assertEquals(255, info.locationLabel().length());
    }
}
