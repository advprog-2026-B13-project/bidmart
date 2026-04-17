package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdminRolePermissionUseCase {
    RoleWithPermissionsView createRole(String roleName, Set<PermissionValue> permissions);
    List<RoleWithPermissionsView> listRolesWithPermissions();
    RoleWithPermissionsView setRolePermissions(int roleId, Set<PermissionValue> permissions);
    void deleteRole(int roleId);
    void assignRoleToUser(UUID userId, int roleId);
    void unassignRoleFromUser(UUID userId);
    UsersForRoleManagementPageView listUsersForRoleManagement(int page, int size);
    List<PermissionValue> listAvailablePermissions();

    record RoleWithPermissionsView(int roleId, String roleName, Set<PermissionValue> permissions) {}

    record UserForRoleManagementView(
            UUID userId,
            String email,
            String displayName,
            UserStatus status,
            Instant createdAt,
            Integer roleId,
            String roleName
    ) {}

    record UsersForRoleManagementPageView(
            List<UserForRoleManagementView> users,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {}
}
