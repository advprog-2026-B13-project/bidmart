package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionValueTest {

    @Test
    void fromName_validName_shouldReturnEnum() {
        assertEquals(PermissionValue.ADMIN, PermissionValue.fromName("admin:all"));
        assertEquals(PermissionValue.AUCTION_CREATE, PermissionValue.fromName("auction:create"));
    }

    @Test
    void fromName_invalidName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PermissionValue.fromName("invalid:perm"));
    }

    @Test
    void getPermissionName_shouldReturnName() {
        assertEquals("admin:all", PermissionValue.ADMIN.getPermissionName());
        assertEquals("auction:create", PermissionValue.AUCTION_CREATE.getPermissionName());
    }

    @Test
    void allValues_shouldHavePermissionName() {
        for (PermissionValue pv : PermissionValue.values()) {
            assertNotNull(pv.getPermissionName());
            assertFalse(pv.getPermissionName().isEmpty());
        }
    }
}
