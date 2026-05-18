package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import java.util.Optional;
import java.util.UUID;

public interface SessionReplacementRequestPort {
    void save(String replacementToken, UUID userId, String deviceInfo, String ipAddress, String locationLabel, long ttlSeconds);
    Optional<SessionReplacementRequestData> get(String replacementToken);
    void delete(String replacementToken);

    record SessionReplacementRequestData(UUID userId, String deviceInfo, String ipAddress, String locationLabel) {}
}

