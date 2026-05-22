package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevEmailTestControllerTest {

    @Mock private EmailOtpSenderPort emailOtpSenderPort;

    private DevEmailTestController controller;

    @BeforeEach
    void setUp() {
        controller = new DevEmailTestController(emailOtpSenderPort);
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "http://localhost:3000");
    }

    @Test
    void sendTestOtpEmail_success() {
        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("test@example.com");

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailOtpSenderPort).sendOtpEmail(eq("test@example.com"), eq("123456"), eq(600L), anyString());
    }

    @Test
    void sendTestOtpEmail_nullRequest_shouldReturn400() {
        var response = controller.sendTestOtpEmail(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void sendTestOtpEmail_emptyEmail_shouldReturn400() {
        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("");

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void sendTestOtpEmail_noAtSign_shouldReturn400() {
        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("not-email");

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void sendTestOtpEmail_customOtpAndTtl() {
        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("test@example.com");
        req.setOtp("654321");
        req.setTtlSeconds(300L);

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailOtpSenderPort).sendOtpEmail(eq("test@example.com"), eq("654321"), eq(300L), anyString());
    }

    @Test
    void sendTestOtpEmail_zeroTtl_shouldUseDefault() {
        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("test@example.com");
        req.setTtlSeconds(0L);

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailOtpSenderPort).sendOtpEmail(eq("test@example.com"), anyString(), eq(600L), anyString());
    }

    @Test
    void sendTestOtpEmail_noVerificationLink() {
        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("test@example.com");
        req.setIncludeVerificationLink(false);

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailOtpSenderPort).sendOtpEmail(eq("test@example.com"), anyString(), anyLong(), isNull());
    }

    @Test
    void sendTestOtpEmail_senderFails_shouldReturn503() {
        doThrow(new IllegalStateException("SMTP down", new RuntimeException("conn refused")))
                .when(emailOtpSenderPort).sendOtpEmail(anyString(), anyString(), anyLong(), anyString());

        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("test@example.com");

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("conn refused"));
    }

    @Test
    void sendTestOtpEmail_senderFailsNoCause_shouldReturn503() {
        doThrow(new IllegalStateException("SMTP down"))
                .when(emailOtpSenderPort).sendOtpEmail(anyString(), anyString(), anyLong(), anyString());

        DevSendEmailRequest req = new DevSendEmailRequest();
        req.setEmail("test@example.com");

        var response = controller.sendTestOtpEmail(req);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("SMTP down"));
    }
}
