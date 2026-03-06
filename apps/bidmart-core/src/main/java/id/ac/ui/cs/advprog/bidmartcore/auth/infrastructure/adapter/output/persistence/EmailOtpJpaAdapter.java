package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.EmailOtpSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailOtpJpaAdapter implements EmailOtpRepositoryPort {
    private final EmailOtpSpringRepository springRepository;

    @Override
    public EmailOtp save(EmailOtp emailOtp) {
        return springRepository.save(emailOtp);
    }

    @Override
    public Optional<EmailOtp> findLatestActiveByUserId(UUID userId) {
        return springRepository.findLatestActiveByUserId(userId);
    }

    @Override
    @Transactional
    public void invalidateAllByUserId(UUID userId) {
        springRepository.invalidateAllByUserId(userId);
    }
}

