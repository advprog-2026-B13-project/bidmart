package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailOtpSenderPortTest {

    @Test
    void unit_defaultSendOtpEmail_delegatesToFullMethod() {
        boolean[] called = {false};
        EmailOtpSenderPort port = new EmailOtpSenderPort() {
            @Override
            public void sendOtpEmail(String recipientEmail, String otpCode, long otpTtlSeconds, String verificationUrl) {
                called[0] = true;
                assertEquals("test@test.com", recipientEmail);
                assertEquals("123456", otpCode);
                assertEquals(300L, otpTtlSeconds);
                assertNull(verificationUrl);
            }
        };

        port.sendOtpEmail("test@test.com", "123456", 300L);

        assertTrue(called[0], "Default method should delegate to full version with null verificationUrl");
    }
}
