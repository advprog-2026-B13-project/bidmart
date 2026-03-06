package id.ac.ui.cs.advprog.bidmartcore.notification.controller;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    @Autowired
    public NotificationPreferenceController(NotificationPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<NotificationPreference> getPreferences(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getPreferenceByUserId(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @PathVariable UUID userId,
            @RequestParam boolean emailEnabled,
            @RequestParam boolean pushEnabled) {

        NotificationPreference updatedPref = service.updatePreference(userId, emailEnabled, pushEnabled);
        return ResponseEntity.ok(updatedPref);
    }
}