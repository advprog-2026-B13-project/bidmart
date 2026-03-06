package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import java.util.Map;
import java.util.UUID;

public interface MfaUseCase {
    Map<String, Object> setupTotp(UUID userId);
    void confirmTotpSetup(UUID userId, String code);
    void enableEmailMfa(UUID userId);
    void disableMfa(UUID userId);
    void requestEmailOtp(String preAuthToken);
    Map<String, Object> verifyMfa(String preAuthToken, String code);
}

