package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpSpringRepository extends JpaRepository<EmailOtp, UUID> {

    @Query("SELECT e FROM EmailOtp e WHERE e.user.id = :userId AND e.isUsed = false ORDER BY e.createdAt DESC LIMIT 1")
    Optional<EmailOtp> findLatestActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE EmailOtp e SET e.isUsed = true WHERE e.user.id = :userId")
    void invalidateAllByUserId(@Param("userId") UUID userId);
}

