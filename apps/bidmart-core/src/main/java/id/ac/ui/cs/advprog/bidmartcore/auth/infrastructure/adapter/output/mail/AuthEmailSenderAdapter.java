package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.mail;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PasswordResetEmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Primary
@RequiredArgsConstructor
public class AuthEmailSenderAdapter implements EmailOtpSenderPort, PasswordResetEmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${auth.email.from:no-reply@bidmart.store}")
    private String fromAddress;

    @Value("${auth.email.otp-subject:Your BidMart OTP code}")
    private String otpSubject;

    @Value("${auth.email.password-reset-subject:Reset your BidMart password}")
    private String passwordResetSubject;

    @Value("${auth.email.app-name:BidMart}")
    private String appName;

    @Value("${auth.email.resend.api-key:}")
    private String resendApiKey;

    @Value("classpath:templates/email-otp.html")
    private Resource otpTemplate;

    @Value("classpath:templates/email-password-reset.html")
    private Resource resetTemplate;

    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode, long otpTtlSeconds, String verificationUrl) {
        try {
            String htmlBody = buildOtpHtmlBody(otpCode, otpTtlSeconds, verificationUrl);
            sendEmail(recipientEmail, otpSubject, htmlBody);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render OTP email template", e);
        }
    }

    @Override
    public void sendResetEmail(String recipientEmail, String resetUrl, long ttlSeconds) {
        try {
            String htmlBody = buildPasswordResetHtmlBody(resetUrl, ttlSeconds);
            sendEmail(recipientEmail, passwordResetSubject, htmlBody);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render password reset email template", e);
        }
    }

    private void sendEmail(String recipientEmail, String subject, String htmlBody) {
        if (StringUtils.hasText(resendApiKey)) {
            try {
                sendWithResend(recipientEmail, subject, htmlBody);
                return;
            } catch (IllegalStateException e) {
                sendWithSmtp(recipientEmail, subject, htmlBody);
                return;
            }
        }

        sendWithSmtp(recipientEmail, subject, htmlBody);
    }

    private void sendWithSmtp(String recipientEmail, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to send email", e);
        }
    }

    private void sendWithResend(String recipientEmail, String subject, String htmlBody) {
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(recipientEmail)
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            resend.emails().send(params);
            return;
        } catch (Exception ignored) {
            // Fall through to HTTP client.
        }

        try {
            sendWithResendHttp(recipientEmail, subject, htmlBody);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send email via Resend", e);
        }
    }

    private void sendWithResendHttp(String recipientEmail, String subject, String htmlBody) {
        RestClient client = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + resendApiKey)
                .build();

        client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", fromAddress,
                        "to", List.of(recipientEmail),
                        "subject", subject,
                        "html", htmlBody
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String buildOtpHtmlBody(String otpCode, long otpTtlSeconds, String verificationUrl) throws IOException {
        String html = StreamUtils.copyToString(otpTemplate.getInputStream(), StandardCharsets.UTF_8);
        String verificationLinkBlock = buildVerificationLinkBlock(verificationUrl);

        return html
                .replace("{{APP_NAME}}", appName)
                .replace("{{OTP_CODE}}", otpCode)
                .replace("{{OTP_TTL_SECONDS}}", String.valueOf(otpTtlSeconds))
                .replace("{{VERIFICATION_LINK_BLOCK}}", verificationLinkBlock);
    }

    private String buildPasswordResetHtmlBody(String resetUrl, long ttlSeconds) throws IOException {
        String html = StreamUtils.copyToString(resetTemplate.getInputStream(), StandardCharsets.UTF_8);

        return html
                .replace("{{APP_NAME}}", appName)
                .replace("{{RESET_URL}}", resetUrl)
                .replace("{{RESET_TTL_SECONDS}}", String.valueOf(ttlSeconds));
    }

    private String buildVerificationLinkBlock(String verificationUrl) {
        if (!StringUtils.hasText(verificationUrl)) {
            return "";
        }

        return """
                <tr>
                    <td style=\"font-size:14px;line-height:1.6;padding-bottom:12px;\">
                        Or verify instantly by clicking this link:
                    </td>
                </tr>
                <tr>
                    <td align=\"center\" style=\"padding:4px 0 20px 0;\">
                        <a href=\"%s\" style=\"display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:8px;font-size:14px;font-weight:600;\">Verify Email</a>
                    </td>
                </tr>
                <tr>
                    <td style=\"font-size:12px;line-height:1.6;color:#6b7280;padding-bottom:18px;word-break:break-all;\">
                        If the button does not work, open this URL:<br/>%s
                    </td>
                </tr>
                """.formatted(verificationUrl, verificationUrl);
    }
}
