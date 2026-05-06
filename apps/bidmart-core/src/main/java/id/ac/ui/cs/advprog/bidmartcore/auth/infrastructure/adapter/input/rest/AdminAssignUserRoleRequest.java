package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Assign role to user request")
public class AdminAssignUserRoleRequest {

    @Schema(description = "Role ID to assign", example = "2")
    private Integer roleId;
}

