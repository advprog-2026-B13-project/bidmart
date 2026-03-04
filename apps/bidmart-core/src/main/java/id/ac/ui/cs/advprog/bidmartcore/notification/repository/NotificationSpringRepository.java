package id.ac.ui.cs.advprog.bidmartcore.notification.repository;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// TODO: rename atau modif file template ini
public interface NotificationSpringRepository extends JpaRepository<NotificationModel, UUID> {}
