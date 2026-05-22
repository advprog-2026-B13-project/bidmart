package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/admin/rbac")
@RequiredArgsConstructor
@RequirePermission(PermissionValue.ADMIN)
@Tag(name = "Admin RBAC", description = "Admin endpoints for role and permission management")
public class AdminRolePermissionController {

    private final AdminRolePermissionUseCase useCase;

    @GetMapping("/roles")
    @Operation(summary = "List roles with permissions")
    public ResponseEntity<ApiResponse<List<AdminRoleResponse>>> listRoles() {
        List<AdminRoleResponse> roles = useCase.listRolesWithPermissions().stream()
                .map(AdminRoleResponse::fromView)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @PostMapping("/roles")
    @Operation(summary = "Create custom role")
    public ResponseEntity<ApiResponse<AdminRoleResponse>> createRole(@RequestBody AdminCreateRoleRequest request) {
        try {
            var created = useCase.createRole(request.getRoleName(), parsePermissions(request.getPermissions()));
            return ResponseEntity.ok(ApiResponse.success("Role created", AdminRoleResponse.fromView(created)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Replace role permissions")
    public ResponseEntity<ApiResponse<AdminRoleResponse>> setRolePermissions(
            @PathVariable int roleId,
            @RequestBody AdminSetRolePermissionsRequest request) {
        try {
            var updated = useCase.setRolePermissions(roleId, parsePermissions(request.getPermissions()));
            return ResponseEntity.ok(ApiResponse.success("Role permissions updated", AdminRoleResponse.fromView(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{roleId}")
    @Operation(summary = "Delete custom role")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable int roleId) {
        try {
            useCase.deleteRole(roleId);
            return ResponseEntity.ok(ApiResponse.success("Role deleted", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
            @PathVariable UUID userId,
            @RequestBody AdminAssignUserRoleRequest request) {
        try {
            if (request.getRoleId() == null) {
                throw new IllegalArgumentException("Role ID is required");
            }
            useCase.assignRoleToUser(userId, request.getRoleId());
            return ResponseEntity.ok(ApiResponse.success("Role assigned to user", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}/role")
    @Operation(summary = "Unassign role from user")
    public ResponseEntity<ApiResponse<Void>> unassignRoleFromUser(@PathVariable UUID userId) {
        try {
            useCase.unassignRoleFromUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Role unassigned from user", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users")
    @Operation(summary = "List users for role management")
    public ResponseEntity<ApiResponse<AdminManagedUsersPageResponse>> listUsersForRoleManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var users = useCase.listUsersForRoleManagement(page, size);
            return ResponseEntity.ok(ApiResponse.success(AdminManagedUsersPageResponse.fromView(users)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/permissions")
    @Operation(summary = "List available permissions")
    public ResponseEntity<ApiResponse<List<String>>> listPermissions() {
        List<String> permissions = useCase.listAvailablePermissions().stream()
                .map(PermissionValue::getPermissionName)
                .sorted()
                .toList();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    private Set<PermissionValue> parsePermissions(Set<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return Set.of();
        }

        try {
            return permissionNames.stream()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(PermissionValue::fromName)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            List<String> allowed = Arrays.stream(PermissionValue.values())
                    .map(PermissionValue::getPermissionName)
                    .sorted()
                    .toList();
            throw new IllegalArgumentException("Unknown permission name. Allowed values: " + allowed);
        }
    }
}
