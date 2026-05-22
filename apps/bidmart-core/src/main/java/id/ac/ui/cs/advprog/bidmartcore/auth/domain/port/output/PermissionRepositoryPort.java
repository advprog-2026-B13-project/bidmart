package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;

import java.util.List;
import java.util.Optional;

public interface PermissionRepositoryPort {
    Optional<Permission> findByName(PermissionValue permissionValue);
    List<Permission> findAll();
}

