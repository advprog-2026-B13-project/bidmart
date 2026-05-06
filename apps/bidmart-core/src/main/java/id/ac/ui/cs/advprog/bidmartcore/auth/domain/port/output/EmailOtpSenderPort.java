package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

public interface EmailOtpSenderPort {

    default void sendOtpEmail(String recipientEmail, String otpCode, long otpTtlSeconds) {
        sendOtpEmail(recipientEmail, otpCode, otpTtlSeconds, null);
    }

    void sendOtpEmail(String recipientEmail, String otpCode, long otpTtlSeconds, String verificationUrl);
}
