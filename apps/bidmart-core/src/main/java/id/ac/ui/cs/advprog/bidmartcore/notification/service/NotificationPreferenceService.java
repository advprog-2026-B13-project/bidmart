package id.ac.ui.cs.advprog.bidmartcore.notification.service;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationPreference;
import java.util.UUID;

public interface NotificationPreferenceService {
    NotificationPreference getPreferenceByUserId(UUID userId);
    NotificationPreference updatePreference(UUID userId, boolean emailEnabled, boolean pushEnabled);
}