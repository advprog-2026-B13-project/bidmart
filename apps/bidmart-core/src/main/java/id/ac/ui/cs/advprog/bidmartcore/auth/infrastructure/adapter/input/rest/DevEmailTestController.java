package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/email")
@RequiredArgsConstructor
@Tag(name = "Dev Email", description = "Dev-only utilities to validate SMTP delivery")
@Slf4j
public class DevEmailTestController {

    private static final long DEFAULT_TTL_SECONDS = 600L;
    private static final String DEFAULT_OTP = "123456";

    private final EmailOtpSenderPort emailOtpSenderPort;

    @Value("${auth.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @PostMapping("/test-otp")
    @Operation(
            summary = "Send test OTP email (dev profile only)",
            description = "Sends a test OTP email to validate SMTP server connectivity and email template rendering."
    )
    public ResponseEntity<ApiResponse<Void>> sendTestOtpEmail(@RequestBody DevSendEmailRequest request) {
        log.info("Received dev test OTP email request for {}", request != null ? request.getEmail() : null);
        if (request == null || !StringUtils.hasText(request.getEmail()) || !request.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid email is required"));
        }

        String otp = StringUtils.hasText(request.getOtp()) ? request.getOtp() : DEFAULT_OTP;
        long ttlSeconds = request.getTtlSeconds() != null && request.getTtlSeconds() > 0
                ? request.getTtlSeconds()
                : DEFAULT_TTL_SECONDS;

        boolean includeVerificationLink = request.getIncludeVerificationLink() == null
                || request.getIncludeVerificationLink();

        String verificationUrl = includeVerificationLink
                ? buildVerificationUrl(request.getEmail(), otp)
                : null;

        try {
            long startedAt = System.currentTimeMillis();
            emailOtpSenderPort.sendOtpEmail(request.getEmail(), otp, ttlSeconds, verificationUrl);
            log.info("Dev test OTP email sent to {} in {} ms", request.getEmail(), (System.currentTimeMillis() - startedAt));
            return ResponseEntity.ok(ApiResponse.success("Test OTP email sent", null));
        } catch (IllegalStateException e) {
            log.error("Failed to send dev test OTP email to {}", request.getEmail(), e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.status(503).body(ApiResponse.error("Failed to send OTP email: " + message));
        }
    }

    private String buildVerificationUrl(String email, String otp) {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return null;
        }

        String baseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;

        return baseUrl
                + "/verify-email?email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&otp="
                + URLEncoder.encode(otp, StandardCharsets.UTF_8);
    }
}
