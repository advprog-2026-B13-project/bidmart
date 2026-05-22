package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PermissionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RoleRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRolePermissionServiceImpl implements AdminRolePermissionUseCase {

    private static final Set<String> RESERVED_ROLES = Set.of("USER", "ADMIN");
    private static final String ROLE_NOT_FOUND = "Role not found";

    private final RoleRepositoryPort roleRepository;
    private final RolePermissionPort rolePermissionPort;
    private final PermissionRepositoryPort permissionRepository;
    private final UserRepositoryPort userRepository;
    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;

    @Override
    @Transactional
    public RoleWithPermissionsView createRole(String roleName, Set<PermissionValue> permissions) {
        String normalizedName = normalizeRoleName(roleName);
        if (roleRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Role already exists");
        }

        Role role = new Role();
        role.setName(normalizedName);
        Role savedRole = roleRepository.save(role);

        Set<PermissionValue> safePermissions = permissions == null ? Set.of() : permissions;
        validatePermissionsExist(safePermissions);
        rolePermissionPort.replacePermissionsForRole(savedRole.getId(), safePermissions);

        return toView(savedRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleWithPermissionsView> listRolesWithPermissions() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getId))
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public RoleWithPermissionsView setRolePermissions(int roleId, Set<PermissionValue> permissions) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException(ROLE_NOT_FOUND));

        Set<PermissionValue> safePermissions = permissions == null ? Set.of() : permissions;
        validatePermissionsExist(safePermissions);
        rolePermissionPort.replacePermissionsForRole(roleId, safePermissions);

        return toView(role);
    }

    @Override
    @Transactional
    public void deleteRole(int roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException(ROLE_NOT_FOUND));

        String roleName = role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT);
        if (RESERVED_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Reserved role cannot be deleted");
        }

        if (userRepository.countByRoleId(roleId) > 0) {
            throw new IllegalArgumentException("Role is still assigned to users");
        }

        rolePermissionPort.deleteAllByRoleId(roleId);
        roleRepository.deleteById(roleId);
    }

    @Override
    @Transactional
    public void assignRoleToUser(UUID userId, int roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException(ROLE_NOT_FOUND));

        user.setRole(role);
        userRepository.save(user);
        evictUserSessions(userId);
    }

    @Override
    @Transactional
    public void unassignRoleFromUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(null);
        userRepository.save(user);
        evictUserSessions(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UsersForRoleManagementPageView listUsersForRoleManagement(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        UserRepositoryPort.UserPage userPage = userRepository.findUsersPage(page, size);
        List<UserForRoleManagementView> users = userPage.users().stream()
                .map(user -> new UserForRoleManagementView(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        user.getRole() != null ? user.getRole().getId() : null,
                        user.getRole() != null ? user.getRole().getName() : null
                ))
                .toList();

        return new UsersForRoleManagementPageView(
                users,
                userPage.page(),
                userPage.size(),
                userPage.totalElements(),
                userPage.totalPages(),
                userPage.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionValue> listAvailablePermissions() {
        return permissionRepository.findAll().stream()
                .map(id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission::getName)
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    private RoleWithPermissionsView toView(Role role) {
        Set<PermissionValue> permissions = rolePermissionPort.findPermissionsByRoleId(role.getId());
        return new RoleWithPermissionsView(role.getId(), role.getName(), permissions);
    }

    private String normalizeRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new IllegalArgumentException("Role name is required");
        }
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePermissionsExist(Set<PermissionValue> permissions) {
        Set<PermissionValue> existing = permissionRepository.findAll().stream()
                .map(id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission::getName)
                .collect(Collectors.toSet());

        List<PermissionValue> missing = permissions.stream()
                .filter(permission -> !existing.contains(permission))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Permissions not found: " + missing);
        }
    }

    private void evictUserSessions(UUID userId) {
        // Force permission refresh by evicting session cache entries of affected user.
        List<Session> sessions = sessionRepository.findAllByUserId(userId);
        for (Session session : sessions) {
            sessionCache.evictSession(session.getId());
        }
    }
}
