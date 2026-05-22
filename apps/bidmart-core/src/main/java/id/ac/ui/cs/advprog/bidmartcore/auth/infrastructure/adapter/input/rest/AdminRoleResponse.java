package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase.RoleWithPermissionsView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role with permissions payload")
public class AdminRoleResponse {

    @Schema(description = "Role ID", example = "2")
    private Integer roleId;

    @Schema(description = "Role name", example = "MODERATOR")
    private String roleName;

    @Schema(description = "Permission names assigned to this role")
    private List<String> permissions;

    public static AdminRoleResponse fromView(RoleWithPermissionsView view) {
        Set<PermissionValue> values = view.permissions();
        List<String> permissions = values == null
                ? List.of()
                : values.stream().map(PermissionValue::getPermissionName).sorted().toList();
        return new AdminRoleResponse(view.roleId(), view.roleName(), permissions);
    }
}

