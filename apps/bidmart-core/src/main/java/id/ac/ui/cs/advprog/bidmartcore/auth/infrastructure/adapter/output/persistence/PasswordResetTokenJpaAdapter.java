package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PasswordResetTokenRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.PasswordResetTokenSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenRepositoryPort {
    private final PasswordResetTokenSpringRepository springRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return springRepository.save(token);
    }

    @Override
    public Optional<PasswordResetToken> findById(UUID id) {
        return springRepository.findById(id);
    }

    @Override
    @Transactional
    public void invalidateAllByUserId(UUID userId) {
        springRepository.invalidateAllByUserId(userId);
    }
}

