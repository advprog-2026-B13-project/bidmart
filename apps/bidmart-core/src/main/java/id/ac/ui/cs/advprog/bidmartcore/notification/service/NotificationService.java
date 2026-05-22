package id.ac.ui.cs.advprog.bidmartcore.notification.service;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<Notification> getUserNotifications(UUID userId);
    void createNotification(UUID userId, String type, String message, UUID referenceId);
    void markAsRead(UUID notificationId);
    void markAllAsRead(UUID userId);
}