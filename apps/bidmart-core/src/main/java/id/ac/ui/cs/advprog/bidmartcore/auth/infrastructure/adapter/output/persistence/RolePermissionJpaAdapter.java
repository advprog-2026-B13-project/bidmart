package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.RolePermission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.embeddable.RolePermissionKey;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PermissionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RolePermissionSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RoleSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RolePermissionJpaAdapter implements RolePermissionPort {
    private final RolePermissionSpringRepository springRepository;
    private final RoleSpringRepository roleSpringRepository;
    private final PermissionRepositoryPort permissionRepository;

    @Override
    public Set<PermissionValue> findPermissionsByRoleId(int roleId) {
        return springRepository.findAllByRoleId(roleId).stream()
                .map(rp -> rp.getId().getPermission().getName())
                .collect(Collectors.toSet());
    }

    @Override
    public void replacePermissionsForRole(int roleId, Set<PermissionValue> permissions) {
        springRepository.deleteAllByRoleId(roleId);

        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        Role role = roleSpringRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        var rolePermissions = new ArrayList<RolePermission>();
        for (PermissionValue permissionValue : permissions) {
            Permission permission = permissionRepository.findByName(permissionValue)
                    .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionValue));
            rolePermissions.add(new RolePermission(new RolePermissionKey(role, permission)));
        }

        springRepository.saveAll(rolePermissions);
    }

    @Override
    public void deleteAllByRoleId(int roleId) {
        springRepository.deleteAllByRoleId(roleId);
    }
}
