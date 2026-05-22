package id.ac.ui.cs.advprog.bidmartcore.notification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationGettersSettersAndConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String type = "OUTBID";
        String message = "You've been outbid!";
        UUID referenceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(true);
        notification.setReferenceId(referenceId);
        notification.setCreatedAt(createdAt);

        assertEquals(id, notification.getId());
        assertEquals(userId, notification.getUserId());
        assertEquals(type, notification.getType());
        assertEquals(message, notification.getMessage());
        assertTrue(notification.isRead());
        assertEquals(referenceId, notification.getReferenceId());
        assertEquals(createdAt, notification.getCreatedAt());

        Notification notification2 = new Notification(id, userId, type, message, false, referenceId, createdAt);
        assertEquals(id, notification2.getId());
        assertEquals(userId, notification2.getUserId());
        assertEquals(type, notification2.getType());
        assertEquals(message, notification2.getMessage());
        assertFalse(notification2.isRead());
        assertEquals(referenceId, notification2.getReferenceId());
        assertEquals(createdAt, notification2.getCreatedAt());
    }
}