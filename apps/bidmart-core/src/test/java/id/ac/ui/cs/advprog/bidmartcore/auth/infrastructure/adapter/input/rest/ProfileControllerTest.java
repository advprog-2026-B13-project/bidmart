package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.ProfileUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock private ProfileUseCase profileUseCase;
    @Mock private AuthContext authContext;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(profileUseCase, authContext);
    }

    private User createUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setDisplayName("Test");
        user.setStatus(UserStatus.ACTIVE);
        user.setDefault2FAMethod(MFAType.DISABLED);
        user.setCreatedAt(Instant.now());
        user.setRole(new Role());
        return user;
    }

    @Test
    void getProfile_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        when(profileUseCase.getProfile(userId)).thenReturn(createUser(userId));

        var response = controller.getProfile();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getData().getUserId());
    }

    @Test
    void updateProfile_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        User updated = createUser(userId);
        updated.setDisplayName("New Name");
        when(profileUseCase.updateProfile(userId, "New Name", null, null)).thenReturn(updated);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setDisplayName("New Name");

        var response = controller.updateProfile(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateProfile_invalidInput_shouldReturn400() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        when(profileUseCase.updateProfile(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid"));

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setDisplayName("name");
        var response = controller.updateProfile(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deactivateAccount_success() {
        UUID targetId = UUID.randomUUID();
        var response = controller.deactivateAccount(targetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileUseCase).deactivateAccount(targetId);
    }

    @Test
    void deactivateAccount_notFound_shouldReturn400() {
        UUID targetId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Not found")).when(profileUseCase).deactivateAccount(targetId);

        var response = controller.deactivateAccount(targetId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getOtherUserProfile_admin_shouldIncludeBids() {
        UUID targetId = UUID.randomUUID();
        when(authContext.hasPermission(PermissionValue.ADMIN)).thenReturn(true);

        var bidView = new ProfileUseCase.BidView(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100), "ACCEPTED", LocalDateTime.now());
        var view = new ProfileUseCase.OtherUserProfileView(
                createUser(targetId), List.of(), List.of(bidView));
        when(profileUseCase.getOtherUserProfile(targetId, true)).thenReturn(view);

        var response = controller.getOtherUserProfile(targetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isBiddingHistoryVisible());
    }

    @Test
    void getOtherUserProfile_nonAdmin_shouldHideBids() {
        UUID targetId = UUID.randomUUID();
        when(authContext.hasPermission(PermissionValue.ADMIN)).thenReturn(false);

        var view = new ProfileUseCase.OtherUserProfileView(
                createUser(targetId), List.of(), List.of());
        when(profileUseCase.getOtherUserProfile(targetId, false)).thenReturn(view);

        var response = controller.getOtherUserProfile(targetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getData().isBiddingHistoryVisible());
    }

    @Test
    void getOtherUserProfile_notFound_shouldReturn400() {
        UUID targetId = UUID.randomUUID();
        when(authContext.hasPermission(any())).thenReturn(true);
        when(profileUseCase.getOtherUserProfile(eq(targetId), anyBoolean()))
                .thenThrow(new IllegalArgumentException("Not found"));

        var response = controller.getOtherUserProfile(targetId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
