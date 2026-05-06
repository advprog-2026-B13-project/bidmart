package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

public record SessionClientInfo(String deviceInfo, String ipAddress, String locationLabel) {

    private static final String DEFAULT_DEVICE = "Unknown device";
    private static final String DEFAULT_IP = "unknown";
    private static final String DEFAULT_LOCATION = "Unknown location";

    public static SessionClientInfo of(String deviceInfo, String ipAddress, String locationLabel) {
        return new SessionClientInfo(
                sanitize(deviceInfo, DEFAULT_DEVICE, 512),
                sanitize(ipAddress, DEFAULT_IP, 64),
                sanitize(locationLabel, DEFAULT_LOCATION, 255)
        );
    }

    public static SessionClientInfo unknown() {
        return of(null, null, null);
    }

    private static String sanitize(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}

