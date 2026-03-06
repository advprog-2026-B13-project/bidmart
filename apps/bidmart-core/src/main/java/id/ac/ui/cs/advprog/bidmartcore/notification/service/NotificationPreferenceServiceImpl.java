package id.ac.ui.cs.advprog.bidmartcore.notification.service;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartcore.notification.repository.NotificationPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    @Autowired
    public NotificationPreferenceServiceImpl(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            NotificationPreference newPref = new NotificationPreference();
            newPref.setUserId(userId);
            newPref.setEmailEnabled(true);
            newPref.setPushEnabled(true);
            return repository.save(newPref);
        });
    }

    @Override
    public NotificationPreference updatePreference(UUID userId, boolean emailEnabled, boolean pushEnabled) {
        NotificationPreference pref = getPreferenceByUserId(userId);
        pref.setEmailEnabled(emailEnabled);
        pref.setPushEnabled(pushEnabled);
        return repository.save(pref);
    }
}