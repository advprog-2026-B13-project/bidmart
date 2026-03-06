package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import java.util.Optional;
import java.util.UUID;

public interface PreAuthSessionPort {
    void save(String preAuthToken, UUID userId, String mfaType, long ttlSeconds);
    Optional<PreAuthSessionData> get(String preAuthToken);
    void delete(String preAuthToken);

    record PreAuthSessionData(UUID userId, String mfaType) {}
}

