package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.TotpCredential;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.TotpCredentialRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.TotpCredentialSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TotpCredentialJpaAdapter implements TotpCredentialRepositoryPort {
    private final TotpCredentialSpringRepository springRepository;

    @Override
    public TotpCredential save(TotpCredential credential) {
        return springRepository.save(credential);
    }

    @Override
    public Optional<TotpCredential> findByUserId(UUID userId) {
        return springRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        springRepository.deleteByUserId(userId);
    }
}

