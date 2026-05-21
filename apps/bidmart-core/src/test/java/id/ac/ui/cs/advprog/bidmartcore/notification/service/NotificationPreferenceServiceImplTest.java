package id.ac.ui.cs.advprog.bidmartcore.notification.service;

import id.ac.ui.cs.advprog.bidmartcore.notification.dto.NotificationPreferenceDTO;
import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartcore.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationPreferenceRepository repository;

    @InjectMocks
    private NotificationPreferenceServiceImpl service;

    private UUID userId;
    private NotificationPreference existingPreference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        existingPreference = new NotificationPreference();
        existingPreference.setId(UUID.randomUUID());
        existingPreference.setUserId(userId);
        existingPreference.setEmailEnabled(false);
        existingPreference.setPushEnabled(true);
    }

    @Test
    void getPreferenceByUserId_whenPreferenceExists_shouldReturnIt() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));

        NotificationPreference result = service.getPreferenceByUserId(userId);

        assertNotNull(result);
        assertEquals(existingPreference.getId(), result.getId());
        assertEquals(userId, result.getUserId());
        assertFalse(result.isEmailEnabled());
        assertTrue(result.isPushEnabled());
        verify(repository, never()).save(any(NotificationPreference.class));
    }

    @Test
    void getPreferenceByUserId_whenPreferenceDoesNotExist_shouldCreateAndReturnDefault() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = service.getPreferenceByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.isEmailEnabled()); // Default is true
        assertTrue(result.isPushEnabled());  // Default is true
        verify(repository, times(1)).save(any(NotificationPreference.class));
    }

    @Test
    void updatePreference_shouldUpdateFieldsAndSave() {
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO(true, false);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = service.updatePreference(userId, dto);

        assertNotNull(result);
        assertEquals(existingPreference.getId(), result.getId());
        assertEquals(userId, result.getUserId());
        assertTrue(result.isEmailEnabled());
        assertFalse(result.isPushEnabled());
        verify(repository, times(1)).save(existingPreference);
    }
}
