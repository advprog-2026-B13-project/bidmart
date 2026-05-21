package id.ac.ui.cs.advprog.bidmartcore.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartcore.notification.dto.NotificationPreferenceDTO;
import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationPreferenceService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationPreferenceController(service)).build();
    }

    @Test
    void getPreferences_shouldReturnPreference() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference();
        preference.setId(UUID.randomUUID());
        preference.setUserId(userId);
        preference.setEmailEnabled(true);
        preference.setPushEnabled(false);

        when(service.getPreferenceByUserId(userId)).thenReturn(preference);

        mockMvc.perform(get("/api/notifications/preferences/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.pushEnabled").value(false));
    }

    @Test
    void updatePreferences_shouldReturnUpdatedPreference() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO(false, true);

        NotificationPreference updatedPref = new NotificationPreference();
        updatedPref.setId(UUID.randomUUID());
        updatedPref.setUserId(userId);
        updatedPref.setEmailEnabled(false);
        updatedPref.setPushEnabled(true);

        when(service.updatePreference(eq(userId), any(NotificationPreferenceDTO.class))).thenReturn(updatedPref);

        mockMvc.perform(put("/api/notifications/preferences/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.pushEnabled").value(true));
    }
}
