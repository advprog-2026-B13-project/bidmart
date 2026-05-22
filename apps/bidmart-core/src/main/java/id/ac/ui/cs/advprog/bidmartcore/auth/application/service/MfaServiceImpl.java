package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.TotpCredential;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.MfaUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.*;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PreAuthSessionPort.PreAuthSessionData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaUseCase {

    private final TotpCredentialRepositoryPort totpRepository;
    private final EmailOtpRepositoryPort emailOtpRepository;
    private final UserRepositoryPort userRepository;
    private final PreAuthSessionPort preAuthSessionPort;
    private final SessionUseCase sessionUseCase;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpSenderPort emailOtpSenderPort;

    private static final String USER_NOT_FOUND = USER_NOT_FOUND;

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.mfa.pre-auth-ttl-seconds:300}")
    private long preAuthTtlSeconds;

    @Override
    @Transactional
    public Map<String, Object> setupTotp(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        // Remove existing inactive TOTP credentials
        totpRepository.findByUserId(userId).ifPresent(existing -> {
            if (!existing.isActive()) {
                totpRepository.deleteByUserId(userId);
            } else {
                throw new IllegalStateException("TOTP is already set up and active");
            }
        });

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();

        TotpCredential credential = new TotpCredential();
        credential.setUser(user);
        credential.setSecretKey(key.getKey());
        credential.setActive(false);
        credential.setCreatedAt(Instant.now());
        totpRepository.save(credential);

        String otpAuthUrl = String.format("otpauth://totp/BidMart:%s?secret=%s&issuer=BidMart",
                user.getEmail(), key.getKey());

        Map<String, Object> result = new HashMap<>();
        result.put("secret", key.getKey());
        result.put("otpAuthUrl", otpAuthUrl);
        return result;
    }

    @Override
    @Transactional
    public void confirmTotpSetup(UUID userId, String code) {
        TotpCredential credential = totpRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No TOTP setup found"));

        if (credential.isActive()) {
            throw new IllegalStateException("TOTP is already active");
        }

        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid TOTP code format");
        }

        boolean valid = googleAuthenticator.authorize(credential.getSecretKey(), codeInt);
        if (!valid) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }

        credential.setActive(true);
        totpRepository.save(credential);

        User user = credential.getUser();
        user.setDefault2FAMethod(MFAType.TOTP);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void enableEmailMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        user.setDefault2FAMethod(MFAType.EMAIL);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void disableMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        user.setDefault2FAMethod(MFAType.DISABLED);
        userRepository.save(user);

        totpRepository.deleteByUserId(userId);
        emailOtpRepository.invalidateAllByUserId(userId);
    }

    @Override
    @Transactional
    public void requestEmailOtp(String preAuthToken) {
        PreAuthSessionData data = preAuthSessionPort.get(preAuthToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired pre-auth token"));

        User user = userRepository.findById(data.userId())
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        // Invalidate any existing OTPs
        emailOtpRepository.invalidateAllByUserId(user.getId());

        // Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setUser(user);
        emailOtp.setOtpHash(passwordEncoder.encode(otp));
        emailOtp.setExpiresAt(Instant.now().plusSeconds(preAuthTtlSeconds));
        emailOtp.setUsed(false);
        emailOtp.setCreatedAt(Instant.now());
        emailOtpRepository.save(emailOtp);

        emailOtpSenderPort.sendOtpEmail(user.getEmail(), otp, preAuthTtlSeconds);
    }

    @Override
    @Transactional
    public Map<String, Object> verifyMfa(String preAuthToken, String code, SessionClientInfo clientInfo) {
        PreAuthSessionData data = preAuthSessionPort.get(preAuthToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired pre-auth token"));

        UUID userId = data.userId();
        String mfaType = data.mfaType();

        if (MFAType.TOTP.name().equals(mfaType)) {
            verifyTotp(userId, code);
        } else if (MFAType.EMAIL.name().equals(mfaType)) {
            verifyEmailOtp(userId, code);
        } else {
            throw new IllegalStateException("Unknown MFA type: " + mfaType);
        }

        // MFA verified - delete pre-auth session and create real session
        preAuthSessionPort.delete(preAuthToken);
        return sessionUseCase.createSession(userId, clientInfo);
    }

    private void verifyTotp(UUID userId, String code) {
        TotpCredential credential = totpRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No TOTP credential found"));

        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid TOTP code format");
        }

        if (!googleAuthenticator.authorize(credential.getSecretKey(), codeInt)) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }
    }

    private void verifyEmailOtp(UUID userId, String code) {
        EmailOtp emailOtp = emailOtpRepository.findLatestActiveByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP found"));

        if (emailOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (!passwordEncoder.matches(code, emailOtp.getOtpHash())) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        emailOtp.setUsed(true);
        emailOtpRepository.save(emailOtp);
    }
}

