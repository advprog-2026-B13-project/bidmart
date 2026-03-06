package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import java.util.Map;

public interface AuthUseCase {
    Map<String, Object> register(String email, String password, String displayName);
    Map<String, Object> login(String email, String password);
    void logout(String sessionId);
    Map<String, Object> refreshToken(String refreshToken);
}
