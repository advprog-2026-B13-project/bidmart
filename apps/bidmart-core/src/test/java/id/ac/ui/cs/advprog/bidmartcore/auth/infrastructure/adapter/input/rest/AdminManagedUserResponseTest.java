package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase.UserForRoleManagementView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminManagedUserResponseTest {

    @Test
    void unit_fromView_mapsAllFields() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        UserForRoleManagementView view = new UserForRoleManagementView(
                userId, "user@test.com", "User One", UserStatus.ACTIVE, createdAt, 2, "SELLER"
        );

        AdminManagedUserResponse response = AdminManagedUserResponse.fromView(view);

        assertEquals(userId, response.getUserId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("User One", response.getDisplayName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(createdAt.toString(), response.getCreatedAt());
        assertEquals(2, response.getRoleId());
        assertEquals("SELLER", response.getRoleName());
    }

    @Test
    void unit_fromView_handlesNullStatus() {
        UserForRoleManagementView view = new UserForRoleManagementView(
                UUID.randomUUID(), "user@test.com", "User", null, null, null, null
        );

        AdminManagedUserResponse response = AdminManagedUserResponse.fromView(view);

        assertNull(response.getStatus());
        assertNull(response.getCreatedAt());
        assertNull(response.getRoleId());
        assertNull(response.getRoleName());
    }

    @Test
    void unit_noArgsConstructorAndGettersSetters() {
        AdminManagedUserResponse response = new AdminManagedUserResponse();
        UUID id = UUID.randomUUID();
        response.setUserId(id);
        response.setEmail("test@test.com");
        response.setDisplayName("Test");
        response.setStatus("ACTIVE");
        response.setCreatedAt("2026-01-01T00:00:00Z");
        response.setRoleId(1);
        response.setRoleName("USER");

        assertEquals(id, response.getUserId());
        assertEquals("test@test.com", response.getEmail());
        assertEquals("Test", response.getDisplayName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("2026-01-01T00:00:00Z", response.getCreatedAt());
        assertEquals(1, response.getRoleId());
        assertEquals("USER", response.getRoleName());
    }

    @Test
    void unit_allArgsConstructor() {
        UUID id = UUID.randomUUID();
        AdminManagedUserResponse response = new AdminManagedUserResponse(
                id, "test@test.com", "Test", "ACTIVE", "2026-01-01T00:00:00Z", 1, "USER"
        );

        assertEquals(id, response.getUserId());
        assertEquals("test@test.com", response.getEmail());
    }
}
