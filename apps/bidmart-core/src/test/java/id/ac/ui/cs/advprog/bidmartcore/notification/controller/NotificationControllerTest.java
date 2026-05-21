package id.ac.ui.cs.advprog.bidmartcore.notification.controller;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.Notification;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService)).build();
    }

    @Test
    void getUserNotifications_shouldReturnNotificationsList() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification1 = new Notification();
        notification1.setId(UUID.randomUUID());
        notification1.setUserId(userId);
        notification1.setType("OUTBID");
        notification1.setMessage("You have been outbid!");
        notification1.setRead(false);
        notification1.setCreatedAt(LocalDateTime.now());

        Notification notification2 = new Notification();
        notification2.setId(UUID.randomUUID());
        notification2.setUserId(userId);
        notification2.setType("BID_WON");
        notification2.setMessage("You won!");
        notification2.setRead(true);
        notification2.setCreatedAt(LocalDateTime.now());

        when(notificationService.getUserNotifications(userId)).thenReturn(Arrays.asList(notification1, notification2));

        mockMvc.perform(get("/api/notifications/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].type").value("OUTBID"))
                .andExpect(jsonPath("$[0].message").value("You have been outbid!"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[1].type").value("BID_WON"))
                .andExpect(jsonPath("$[1].message").value("You won!"))
                .andExpect(jsonPath("$[1].read").value(true));
    }

    @Test
    void getUserNotifications_whenEmpty_shouldReturnEmptyList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationService.getUserNotifications(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/notifications/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }
}
