package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.mail;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Primary
@RequiredArgsConstructor
public class ResendEmailOtpSenderAdapter implements EmailOtpSenderPort {

    @Value("${auth.mfa.email.resend.api-key:}")
    private String resendApiKey;

    @Value("${auth.mfa.email.from:no-reply@bidmart.store}")
    private String fromAddress;

    @Value("${auth.mfa.email.subject:Your BidMart OTP code}")
    private String subject;

    @Value("${auth.mfa.email.app-name:BidMart}")
    private String appName;

    @Value("classpath:templates/email-otp.html")
    private Resource otpTemplate;

    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode, long otpTtlSeconds, String verificationUrl) {
        if (!StringUtils.hasText(resendApiKey)) {
            throw new IllegalStateException("RESEND API key is not configured");
        }

        try {
            String htmlBody = buildHtmlBody(otpCode, otpTtlSeconds, verificationUrl);

            if (!sendWithResendSdk(recipientEmail, htmlBody)) {
                sendWithResendHttp(recipientEmail, htmlBody);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render OTP email template", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send OTP email via Resend", e);
        }
    }

    private boolean sendWithResendSdk(String recipientEmail, String htmlBody) {
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(recipientEmail)
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            resend.emails().send(params);
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Resend SDK failed", e);
        }
    }

    private Object invokeBuilder(Object builder, String methodName, String value) throws Exception {
        Method method = builder.getClass().getMethod(methodName, String.class);
        return method.invoke(builder, value);
    }

    private void sendWithResendHttp(String recipientEmail, String htmlBody) {
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
