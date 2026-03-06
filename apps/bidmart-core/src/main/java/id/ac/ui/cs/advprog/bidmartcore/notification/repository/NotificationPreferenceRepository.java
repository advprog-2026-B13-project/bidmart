package id.ac.ui.cs.advprog.bidmartcore.notification.repository;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByUserId(UUID userId);
}