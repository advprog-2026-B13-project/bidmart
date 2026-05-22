package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.RolePermission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.embeddable.RolePermissionKey;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PermissionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RolePermissionSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RoleSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionJpaAdapterTest {

    @Mock
    private RolePermissionSpringRepository springRepository;

    @Mock
    private RoleSpringRepository roleSpringRepository;

    @Mock
    private PermissionRepositoryPort permissionRepository;

    private RolePermissionJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new RolePermissionJpaAdapter(springRepository, roleSpringRepository, permissionRepository);
    }

    @Test
    void unit_findPermissionsByRoleId() {
        Role role = new Role();
        Permission perm = new Permission();
        perm.setName(PermissionValue.ADMIN);
        RolePermissionKey key = new RolePermissionKey(role, perm);
        RolePermission rp = new RolePermission(key);

        when(springRepository.findAllByRoleId(1)).thenReturn(List.of(rp));

        Set<PermissionValue> result = adapter.findPermissionsByRoleId(1);

        assertEquals(1, result.size());
        assertTrue(result.contains(PermissionValue.ADMIN));
    }

    @Test
    void unit_replacePermissionsForRole_withPermissions() {
        Role role = new Role();
        Permission perm = new Permission();
        perm.setName(PermissionValue.ADMIN);

        when(roleSpringRepository.findById(1)).thenReturn(Optional.of(role));
        when(permissionRepository.findByName(PermissionValue.ADMIN)).thenReturn(Optional.of(perm));

        adapter.replacePermissionsForRole(1, Set.of(PermissionValue.ADMIN));

        verify(springRepository).deleteAllByRoleId(1);
        verify(springRepository).saveAll(any(List.class));
    }

    @Test
    void unit_replacePermissionsForRole_nullPermissions_onlyDeletes() {
        adapter.replacePermissionsForRole(1, null);

        verify(springRepository).deleteAllByRoleId(1);
        verify(springRepository, never()).saveAll(any());
        verify(roleSpringRepository, never()).findById(anyInt());
    }

    @Test
    void unit_replacePermissionsForRole_emptyPermissions_onlyDeletes() {
        adapter.replacePermissionsForRole(1, Set.of());

        verify(springRepository).deleteAllByRoleId(1);
        verify(springRepository, never()).saveAll(any());
    }

    @Test
    void unit_replacePermissionsForRole_roleNotFound_throws() {
        when(roleSpringRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> adapter.replacePermissionsForRole(99, Set.of(PermissionValue.ADMIN)));
    }

    @Test
    void unit_replacePermissionsForRole_permissionNotFound_throws() {
        Role role = new Role();
        when(roleSpringRepository.findById(1)).thenReturn(Optional.of(role));
        when(permissionRepository.findByName(PermissionValue.ADMIN)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> adapter.replacePermissionsForRole(1, Set.of(PermissionValue.ADMIN)));
    }

    @Test
    void unit_deleteAllByRoleId() {
        adapter.deleteAllByRoleId(1);

        verify(springRepository).deleteAllByRoleId(1);
    }
}
