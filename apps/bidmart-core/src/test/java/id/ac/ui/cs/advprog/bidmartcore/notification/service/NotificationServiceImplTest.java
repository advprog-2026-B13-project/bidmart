package id.ac.ui.cs.advprog.bidmartcore.notification.service;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.Notification;
import id.ac.ui.cs.advprog.bidmartcore.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private Notification notification1;
    private Notification notification2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        notification1 = new Notification();
        notification1.setId(UUID.randomUUID());
        notification1.setUserId(userId);
        notification1.setType("OUTBID");
        notification1.setMessage("You have been outbid!");
        notification1.setRead(false);
        notification1.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        notification2 = new Notification();
        notification2.setId(UUID.randomUUID());
        notification2.setUserId(userId);
        notification2.setType("BID_WON");
        notification2.setMessage("Congratulations, you won the bid!");
        notification2.setRead(true);
        notification2.setCreatedAt(LocalDateTime.now());

        ReflectionTestUtils.setField(notificationService, "messagingTemplate", messagingTemplate);
    }

    @Test
    void getUserNotifications_shouldReturnOrderedNotifications() {
        List<Notification> expectedNotifications = Arrays.asList(notification2, notification1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(expectedNotifications);

        List<Notification> actualNotifications = notificationService.getUserNotifications(userId);

        assertNotNull(actualNotifications);
        assertEquals(2, actualNotifications.size());
        assertEquals(notification2, actualNotifications.get(0));
        assertEquals(notification1, actualNotifications.get(1));
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void createNotification_shouldSaveAndBroadcastNotification() {
        String type = "OUTBID";
        String message = "You have been outbid!";

        Notification mockSaved = new Notification();
        mockSaved.setId(UUID.randomUUID());
        mockSaved.setUserId(userId);
        mockSaved.setType(type);
        mockSaved.setMessage(message);
        mockSaved.setRead(false);
        mockSaved.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSaved);

        notificationService.createNotification(userId, type, message);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedInput = notificationCaptor.getValue();
        assertEquals(userId, savedInput.getUserId());
        assertEquals(type, savedInput.getType());
        assertEquals(message, savedInput.getMessage());
        assertFalse(savedInput.isRead());

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/notifications/" + userId),
                eq(mockSaved)
        );
    }
}
