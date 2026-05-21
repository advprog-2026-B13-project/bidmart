package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRolePermissionControllerTest {

    @Mock private AdminRolePermissionUseCase useCase;

    private AdminRolePermissionController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminRolePermissionController(useCase);
    }

    @Test
    void listRoles_shouldReturnOk() {
        when(useCase.listRolesWithPermissions()).thenReturn(List.of());

        var response = controller.listRoles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createRole_success() {
        var view = new AdminRolePermissionUseCase.RoleWithPermissionsView(1, "MOD", Set.of(PermissionValue.ADMIN));
        when(useCase.createRole(eq("mod"), anySet())).thenReturn(view);

        AdminCreateRoleRequest req = new AdminCreateRoleRequest();
        req.setRoleName("mod");
        req.setPermissions(Set.of("admin:all"));
        var response = controller.createRole(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("MOD", response.getBody().getData().getRoleName());
    }

    @Test
    void createRole_duplicate_shouldReturn400() {
        when(useCase.createRole(anyString(), anySet()))
                .thenThrow(new IllegalArgumentException("Duplicate"));

        AdminCreateRoleRequest req = new AdminCreateRoleRequest();
        req.setRoleName("mod");
        req.setPermissions(Set.of("admin:all"));
        var response = controller.createRole(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createRole_nullPermissions_shouldPassEmpty() {
        var view = new AdminRolePermissionUseCase.RoleWithPermissionsView(1, "MOD", Set.of());
        when(useCase.createRole("mod", Set.of())).thenReturn(view);

        AdminCreateRoleRequest req = new AdminCreateRoleRequest();
        req.setRoleName("mod");
        req.setPermissions(null);
        var response = controller.createRole(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void setRolePermissions_success() {
        var view = new AdminRolePermissionUseCase.RoleWithPermissionsView(1, "USER", Set.of(PermissionValue.ADMIN));
        when(useCase.setRolePermissions(eq(1), anySet())).thenReturn(view);

        AdminSetRolePermissionsRequest req = new AdminSetRolePermissionsRequest();
        req.setPermissions(Set.of("admin:all"));
        var response = controller.setRolePermissions(1, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void setRolePermissions_notFound_shouldReturn400() {
        when(useCase.setRolePermissions(anyInt(), anySet()))
                .thenThrow(new IllegalArgumentException("Not found"));

        AdminSetRolePermissionsRequest req = new AdminSetRolePermissionsRequest();
        req.setPermissions(Set.of("admin:all"));
        var response = controller.setRolePermissions(999, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteRole_success() {
        var response = controller.deleteRole(2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(useCase).deleteRole(2);
    }

    @Test
    void deleteRole_reserved_shouldReturn400() {
        doThrow(new IllegalArgumentException("Reserved")).when(useCase).deleteRole(1);

        var response = controller.deleteRole(1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void assignRoleToUser_success() {
        UUID userId = UUID.randomUUID();

        AdminAssignUserRoleRequest req = new AdminAssignUserRoleRequest();
        req.setRoleId(2);
        var response = controller.assignRoleToUser(userId, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(useCase).assignRoleToUser(userId, 2);
    }

    @Test
    void assignRoleToUser_nullRoleId_shouldReturn400() {
        UUID userId = UUID.randomUUID();

        AdminAssignUserRoleRequest req = new AdminAssignUserRoleRequest();
        req.setRoleId(null);
        var response = controller.assignRoleToUser(userId, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void assignRoleToUser_notFound_shouldReturn400() {
        UUID userId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Not found")).when(useCase).assignRoleToUser(any(), anyInt());

        AdminAssignUserRoleRequest req = new AdminAssignUserRoleRequest();
        req.setRoleId(999);
        var response = controller.assignRoleToUser(userId, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void unassignRoleFromUser_success() {
        UUID userId = UUID.randomUUID();

        var response = controller.unassignRoleFromUser(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(useCase).unassignRoleFromUser(userId);
    }

    @Test
    void unassignRoleFromUser_notFound_shouldReturn400() {
        UUID userId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Not found")).when(useCase).unassignRoleFromUser(userId);

        var response = controller.unassignRoleFromUser(userId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void listUsersForRoleManagement_success() {
        var page = new AdminRolePermissionUseCase.UsersForRoleManagementPageView(List.of(), 0, 20, 0, 0, false);
        when(useCase.listUsersForRoleManagement(0, 20)).thenReturn(page);

        var response = controller.listUsersForRoleManagement(0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void listUsersForRoleManagement_invalidPage_shouldReturn400() {
        when(useCase.listUsersForRoleManagement(-1, 10))
                .thenThrow(new IllegalArgumentException("Invalid page"));

        var response = controller.listUsersForRoleManagement(-1, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void listPermissions_shouldReturnSorted() {
        when(useCase.listAvailablePermissions()).thenReturn(List.of(PermissionValue.AUCTION_CREATE, PermissionValue.ADMIN));

        var response = controller.listPermissions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void parsePermissions_invalidName_shouldReturn400() {
        AdminCreateRoleRequest req = new AdminCreateRoleRequest();
        req.setRoleName("test");
        req.setPermissions(Set.of("invalid:perm"));
        var response = controller.createRole(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void parsePermissions_emptySet_shouldPass() {
        var view = new AdminRolePermissionUseCase.RoleWithPermissionsView(1, "TEST", Set.of());
        when(useCase.createRole("test", Set.of())).thenReturn(view);

        AdminCreateRoleRequest req = new AdminCreateRoleRequest();
        req.setRoleName("test");
        req.setPermissions(Set.of());
        var response = controller.createRole(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
