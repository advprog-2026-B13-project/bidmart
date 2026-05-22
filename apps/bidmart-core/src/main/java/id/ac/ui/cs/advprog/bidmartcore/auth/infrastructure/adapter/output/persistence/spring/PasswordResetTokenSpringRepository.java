package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PasswordResetTokenSpringRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.isUsed = true WHERE t.user.id = :userId")
    void invalidateAllByUserId(@Param("userId") UUID userId);
}

