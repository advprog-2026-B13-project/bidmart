package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "Create custom role request")
public class AdminCreateRoleRequest {

    @Schema(description = "Role name", example = "MODERATOR")
    private String roleName;

    @Schema(description = "Permissions assigned to role", example = "[\"auction:create\",\"auction:delete\"]")
    private Set<String> permissions;
}

