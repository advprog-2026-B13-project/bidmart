package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;

import java.util.Set;

public interface RolePermissionPort {
    Set<PermissionValue> findPermissionsByRoleId(int roleId);
}

