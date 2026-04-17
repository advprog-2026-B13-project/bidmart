package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "Replace permissions for a role")
public class AdminSetRolePermissionsRequest {

    @Schema(description = "New permissions for role", example = "[\"user:manage\",\"account:deactivate\"]")
    private Set<String> permissions;
}

