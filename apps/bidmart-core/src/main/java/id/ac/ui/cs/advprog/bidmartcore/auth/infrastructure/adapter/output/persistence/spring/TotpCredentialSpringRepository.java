package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.TotpCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TotpCredentialSpringRepository extends JpaRepository<TotpCredential, UUID> {
    Optional<TotpCredential> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}

