package id.ac.ui.cs.advprog.bidmartcore.notification.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceDTOTest {

    @Test
    void testNotificationPreferenceDTOGettersSettersAndConstructor() {
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setEmailEnabled(true);
        dto.setPushEnabled(false);

        assertTrue(dto.isEmailEnabled());
        assertFalse(dto.isPushEnabled());

        NotificationPreferenceDTO dto2 = new NotificationPreferenceDTO(false, true);
        assertFalse(dto2.isEmailEnabled());
        assertTrue(dto2.isPushEnabled());
    }
}
