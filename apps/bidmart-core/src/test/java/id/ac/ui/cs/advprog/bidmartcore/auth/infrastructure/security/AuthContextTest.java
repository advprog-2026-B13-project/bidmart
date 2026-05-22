package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthContextTest {

    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        authContext = new AuthContext();
    }

    @Test
    void hasPermission_whenHasDirectPermission_shouldReturnTrue() {
        authContext.setPermissions(Set.of(PermissionValue.AUCTION_CREATE));

        assertTrue(authContext.hasPermission(PermissionValue.AUCTION_CREATE));
    }

    @Test
    void hasPermission_whenHasAdminPermission_shouldReturnTrue() {
        authContext.setPermissions(Set.of(PermissionValue.ADMIN));

        assertTrue(authContext.hasPermission(PermissionValue.AUCTION_CREATE));
    }

    @Test
    void hasPermission_whenNoMatch_shouldReturnFalse() {
        authContext.setPermissions(Set.of(PermissionValue.AUCTION_CREATE));

        assertFalse(authContext.hasPermission(PermissionValue.ACCOUNT_DEACTIVATE));
    }

    @Test
    void hasPermission_whenNullPermissions_shouldReturnFalse() {
        authContext.setPermissions(null);

        assertFalse(authContext.hasPermission(PermissionValue.ADMIN));
    }

    @Test
    void settersAndGetters() {
        UUID userId = UUID.randomUUID();
        authContext.setUserId(userId);
        authContext.setSessionId("session-1");
        authContext.setAuthenticated(true);

        assertEquals(userId, authContext.getUserId());
        assertEquals("session-1", authContext.getSessionId());
        assertTrue(authContext.isAuthenticated());
    }
}
