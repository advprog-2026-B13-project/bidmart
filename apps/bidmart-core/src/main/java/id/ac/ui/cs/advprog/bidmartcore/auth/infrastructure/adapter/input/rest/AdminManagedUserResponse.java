package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase.UserForRoleManagementView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User row for admin role management")
public class AdminManagedUserResponse {

    @Schema(description = "User ID")
    private UUID userId;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "User account status")
    private String status;

    @Schema(description = "Account creation timestamp")
    private String createdAt;

    @Schema(description = "Assigned role ID")
    private Integer roleId;

    @Schema(description = "Assigned role name")
    private String roleName;

    public static AdminManagedUserResponse fromView(UserForRoleManagementView view) {
        return new AdminManagedUserResponse(
                view.userId(),
                view.email(),
                view.displayName(),
                view.status() != null ? view.status().name() : null,
                view.createdAt() != null ? view.createdAt().toString() : null,
                view.roleId(),
                view.roleName()
        );
    }
}

