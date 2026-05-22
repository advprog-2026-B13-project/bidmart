package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

public interface PasswordResetEmailSenderPort {
    void sendResetEmail(String recipientEmail, String resetUrl, long ttlSeconds);
}

