package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;

import java.util.Optional;
import java.util.UUID;

public interface EmailOtpRepositoryPort {
    EmailOtp save(EmailOtp emailOtp);
    Optional<EmailOtp> findLatestActiveByUserId(UUID userId);
    void invalidateAllByUserId(UUID userId);
}

