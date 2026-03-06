package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.TotpCredential;

import java.util.Optional;
import java.util.UUID;

public interface TotpCredentialRepositoryPort {
    TotpCredential save(TotpCredential credential);
    Optional<TotpCredential> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}

