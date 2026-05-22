package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.mail;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PasswordResetEmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthEmailSenderAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private AuthEmailSenderAdapter adapter;

    private Resource otpTemplate;
    private Resource resetTemplate;

    @BeforeEach
    void unit_setUp() {
        adapter = new AuthEmailSenderAdapter(mailSender);

        String otpHtml = "<html>{{APP_NAME}} {{OTP_CODE}} {{OTP_TTL_SECONDS}} {{VERIFICATION_LINK_BLOCK}}</html>";
        otpTemplate = new ByteArrayResource(otpHtml.getBytes());

        String resetHtml = "<html>{{APP_NAME}} {{RESET_URL}} {{RESET_TTL_SECONDS}}</html>";
        resetTemplate = new ByteArrayResource(resetHtml.getBytes());

        ReflectionTestUtils.setField(adapter, "fromAddress", "no-reply@bidmart.store");
        ReflectionTestUtils.setField(adapter, "otpSubject", "Your OTP");
        ReflectionTestUtils.setField(adapter, "passwordResetSubject", "Reset Password");
        ReflectionTestUtils.setField(adapter, "appName", "BidMart");
        ReflectionTestUtils.setField(adapter, "resendApiKey", "");
        ReflectionTestUtils.setField(adapter, "otpTemplate", otpTemplate);
        ReflectionTestUtils.setField(adapter, "resetTemplate", resetTemplate);
    }

    @Test
    void unit_sendOtpEmail_viaSmtp() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendOtpEmail("user@test.com", "123456", 300L, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void unit_sendOtpEmail_withVerificationUrl() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendOtpEmail("user@test.com", "123456", 300L, "https://example.com/verify");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void unit_sendOtpEmail_withEmptyVerificationUrl_noLinkBlock() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendOtpEmail("user@test.com", "123456", 300L, "");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void unit_sendResetEmail_viaSmtp() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendResetEmail("user@test.com", "https://example.com/reset", 600L);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void unit_sendOtpEmail_smtpThrows_throwsIllegalState() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class,
                () -> adapter.sendOtpEmail("user@test.com", "123456", 300L, null));
    }

    @Test
    void unit_sendResetEmail_smtpThrows_throwsIllegalState() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class,
                () -> adapter.sendResetEmail("user@test.com", "https://example.com/reset", 600L));
    }

    @Test
    void unit_sendOtpEmail_fallsBackToSmtpWhenResendFails() throws MessagingException {
        ReflectionTestUtils.setField(adapter, "resendApiKey", "re_test_key");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendOtpEmail("user@test.com", "123456", 300L, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void unit_sendResetEmail_fallsBackToSmtpWhenResendFails() throws MessagingException {
        ReflectionTestUtils.setField(adapter, "resendApiKey", "re_test_key");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendResetEmail("user@test.com", "https://example.com/reset", 600L);

        verify(mailSender).send(mimeMessage);
    }
}
