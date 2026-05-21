package id.ac.ui.cs.advprog.bidmartcore.notification.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceTest {

    @Test
    void testNotificationPreferenceGettersSettersAndConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        NotificationPreference preference = new NotificationPreference();
        preference.setId(id);
        preference.setUserId(userId);
        preference.setEmailEnabled(false);
        preference.setPushEnabled(false);

        assertEquals(id, preference.getId());
        assertEquals(userId, preference.getUserId());
        assertFalse(preference.isEmailEnabled());
        assertFalse(preference.isPushEnabled());

        NotificationPreference preference2 = new NotificationPreference(id, userId, true, true);
        assertEquals(id, preference2.getId());
        assertEquals(userId, preference2.getUserId());
        assertTrue(preference2.isEmailEnabled());
        assertTrue(preference2.isPushEnabled());
    }
}
