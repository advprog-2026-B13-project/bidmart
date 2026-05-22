package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PermissionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RoleRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort.UserPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRolePermissionServiceImplTest {

    @Mock
    private RoleRepositoryPort roleRepository;

    @Mock
    private RolePermissionPort rolePermissionPort;

    @Mock
    private PermissionRepositoryPort permissionRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private SessionRepositoryPort sessionRepository;

    @Mock
    private SessionCachePort sessionCache;

    private AdminRolePermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminRolePermissionServiceImpl(
                roleRepository,
                rolePermissionPort,
                permissionRepository,
                userRepository,
                sessionRepository,
                sessionCache
        );
    }

    @Test
    void createRole_whenDuplicate_shouldThrow() {
        when(roleRepository.findByName("CUSTOM")).thenReturn(Optional.of(new Role()));

        assertThrows(IllegalArgumentException.class,
                () -> service.createRole("custom", Set.of(PermissionValue.ADMIN)));
    }

    @Test
    void createRole_whenMissingPermission_shouldThrow() {
        when(roleRepository.findByName("CUSTOM")).thenReturn(Optional.empty());
        when(permissionRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.createRole("custom", Set.of(PermissionValue.ADMIN)));
    }

    @Test
    void createRole_whenValid_shouldPersistAndSetPermissions() {
        Role role = new Role();
        role.setId(1);
        role.setName("CUSTOM");
        when(roleRepository.findByName("CUSTOM")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        Permission permission = new Permission();
        permission.setName(PermissionValue.ADMIN);
        when(permissionRepository.findAll()).thenReturn(List.of(permission));
        when(rolePermissionPort.findPermissionsByRoleId(1)).thenReturn(Set.of(PermissionValue.ADMIN));

        var result = service.createRole("custom", Set.of(PermissionValue.ADMIN));

        assertEquals("CUSTOM", result.roleName());
        verify(rolePermissionPort).replacePermissionsForRole(1, Set.of(PermissionValue.ADMIN));
    }

    @Test
    void deleteRole_whenReserved_shouldThrow() {
        Role role = new Role();
        role.setId(1);
        role.setName("ADMIN");
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));

        assertThrows(IllegalArgumentException.class, () -> service.deleteRole(1));
    }

    @Test
    void deleteRole_whenAssigned_shouldThrow() {
        Role role = new Role();
        role.setId(1);
        role.setName("CUSTOM");
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        when(userRepository.countByRoleId(1)).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> service.deleteRole(1));
    }

    @Test
    void deleteRole_whenValid_shouldDelete() {
        Role role = new Role();
        role.setId(1);
        role.setName("CUSTOM");
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        when(userRepository.countByRoleId(1)).thenReturn(0L);

        service.deleteRole(1);

        verify(rolePermissionPort).deleteAllByRoleId(1);
        verify(roleRepository).deleteById(1);
    }

    @Test
    void assignRoleToUser_shouldEvictSessions() {
        UUID userId = UUID.randomUUID();
        Role role = new Role();
        role.setId(2);
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));

        Session session = new Session();
        session.setId("session-id");
        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of(session));

        service.assignRoleToUser(userId, 2);

        assertEquals(role, user.getRole());
        verify(userRepository).save(user);
        verify(sessionCache).evictSession("session-id");
    }

    @Test
    void listUsersForRoleManagement_whenInvalidPage_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> service.listUsersForRoleManagement(-1, 10));
    }

    @Test
    void listUsersForRoleManagement_whenValid_shouldReturnPage() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setDisplayName("User");
        user.setCreatedAt(Instant.now());
        UserPage page = new UserPage(List.of(user), 0, 1, 1L, 1, false);
        when(userRepository.findUsersPage(0, 1)).thenReturn(page);

        var result = service.listUsersForRoleManagement(0, 1);

        assertEquals(1, result.users().size());
        assertEquals("user@example.com", result.users().get(0).email());
    }

    @Test
    void listAvailablePermissions_shouldReturnSorted() {
        Permission p1 = new Permission();
        p1.setName(PermissionValue.ADMIN);
        Permission p2 = new Permission();
        p2.setName(PermissionValue.AUCTION_CREATE);
        when(permissionRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PermissionValue> result = service.listAvailablePermissions();

        assertEquals(List.of(PermissionValue.ADMIN, PermissionValue.AUCTION_CREATE), result);
    }

    @Test
    void listRolesWithPermissions_shouldReturnSorted() {
        Role r1 = new Role();
        r1.setId(2);
        r1.setName("ADMIN");
        Role r2 = new Role();
        r2.setId(1);
        r2.setName("USER");
        when(roleRepository.findAll()).thenReturn(List.of(r2, r1));
        when(rolePermissionPort.findPermissionsByRoleId(anyInt())).thenReturn(Set.of());

        var result = service.listRolesWithPermissions();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).roleId());
        assertEquals(2, result.get(1).roleId());
    }

    @Test
    void setRolePermissions_whenValid_shouldUpdate() {
        Role role = new Role();
        role.setId(1);
        role.setName("USER");
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        Permission perm = new Permission();
        perm.setName(PermissionValue.ADMIN);
        when(permissionRepository.findAll()).thenReturn(List.of(perm));
        when(rolePermissionPort.findPermissionsByRoleId(1)).thenReturn(Set.of(PermissionValue.ADMIN));

        var result = service.setRolePermissions(1, Set.of(PermissionValue.ADMIN));

        assertEquals("USER", result.roleName());
        verify(rolePermissionPort).replacePermissionsForRole(1, Set.of(PermissionValue.ADMIN));
    }

    @Test
    void setRolePermissions_whenRoleNotFound_shouldThrow() {
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.setRolePermissions(999, Set.of(PermissionValue.ADMIN)));
    }

    @Test
    void unassignRoleFromUser_whenValid_shouldSetRoleNull() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Role role = new Role();
        role.setId(1);
        user.setRole(role);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Session session = new Session();
        session.setId("session-id");
        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of(session));

        service.unassignRoleFromUser(userId);

        assertNull(user.getRole());
        verify(userRepository).save(user);
        verify(sessionCache).evictSession("session-id");
    }

    @Test
    void unassignRoleFromUser_whenUserNotFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.unassignRoleFromUser(userId));
    }

    @Test
    void listUsersForRoleManagement_invalidSize_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> service.listUsersForRoleManagement(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.listUsersForRoleManagement(0, 101));
    }

    @Test
    void createRole_withNullPermissions_shouldUseEmptySet() {
        Role role = new Role();
        role.setId(1);
        role.setName("EMPTY");
        when(roleRepository.findByName("EMPTY")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(rolePermissionPort.findPermissionsByRoleId(1)).thenReturn(Set.of());

        var result = service.createRole("empty", null);

        assertEquals("EMPTY", result.roleName());
        verify(rolePermissionPort).replacePermissionsForRole(1, Set.of());
    }

    @Test
    void createRole_withBlankName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> service.createRole("", Set.of()));
    }

    @Test
    void deleteRole_notFound_shouldThrow() {
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteRole(999));
    }

    @Test
    void assignRoleToUser_userNotFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.assignRoleToUser(userId, 1));
    }

    @Test
    void assignRoleToUser_roleNotFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.assignRoleToUser(userId, 999));
    }

    @Test
    void setRolePermissions_withNullPermissions_shouldUseEmptySet() {
        Role role = new Role();
        role.setId(1);
        role.setName("USER");
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        when(rolePermissionPort.findPermissionsByRoleId(1)).thenReturn(Set.of());

        var result = service.setRolePermissions(1, null);

        assertEquals("USER", result.roleName());
        verify(rolePermissionPort).replacePermissionsForRole(1, Set.of());
    }
}

