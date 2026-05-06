package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;

import java.util.Map;

public interface AuthUseCase {
    Map<String, Object> register(String email, String password, String displayName);
    void verifyEmailOtp(String email, String otpCode);
    void resendVerificationOtp(String email, String verificationToken);
    Map<String, Object> login(String email, String password, SessionClientInfo clientInfo);
    Map<String, Object> confirmSessionReplacement(String replacementToken, boolean shouldReplace, SessionClientInfo clientInfo);
    void logout(String sessionId);
    Map<String, Object> refreshToken(String refreshToken);
}
