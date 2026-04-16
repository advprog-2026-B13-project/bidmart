package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.mail;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SmtpEmailOtpSenderAdapter implements EmailOtpSenderPort {

    private final JavaMailSender mailSender;

    @Value("${auth.mfa.email.from:no-reply@bidmart.local}")
    private String fromAddress;

    @Value("${auth.mfa.email.subject:Your BidMart OTP code}")
    private String subject;

    @Value("${auth.mfa.email.app-name:BidMart}")
    private String appName;

    @Value("classpath:templates/email-otp.html")
    private Resource otpTemplate;

    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode, long otpTtlSeconds, String verificationUrl) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(buildHtmlBody(otpCode, otpTtlSeconds, verificationUrl), true);

            System.out.println("Sending OTP email to " + recipientEmail + " with OTP " + otpCode + " and TTL " + otpTtlSeconds + " seconds. Verification URL: " + verificationUrl);
            mailSender.send(message);
        } catch (MailException | IOException | jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to send OTP email", e);
        }
    }

    private String buildHtmlBody(String otpCode, long otpTtlSeconds, String verificationUrl) throws IOException {
        String html = StreamUtils.copyToString(otpTemplate.getInputStream(), StandardCharsets.UTF_8);
        String verificationLinkBlock = buildVerificationLinkBlock(verificationUrl);

        return html
                .replace("{{APP_NAME}}", appName)
                .replace("{{OTP_CODE}}", otpCode)
                .replace("{{OTP_TTL_SECONDS}}", String.valueOf(otpTtlSeconds))
                .replace("{{VERIFICATION_LINK_BLOCK}}", verificationLinkBlock);
    }

    private String buildVerificationLinkBlock(String verificationUrl) {
        if (!StringUtils.hasText(verificationUrl)) {
            return "";
        }

        return """
                <tr>
                    <td style="font-size:14px;line-height:1.6;padding-bottom:12px;">
                        Or verify instantly by clicking this link:
                    </td>
                </tr>
                <tr>
                    <td align="center" style="padding:4px 0 20px 0;">
                        <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:8px;font-size:14px;font-weight:600;">Verify Email</a>
                    </td>
                </tr>
                <tr>
                    <td style="font-size:12px;line-height:1.6;color:#6b7280;padding-bottom:18px;word-break:break-all;">
                        If the button does not work, open this URL:<br/>%s
                    </td>
                </tr>
                """.formatted(verificationUrl, verificationUrl);
    }
}
