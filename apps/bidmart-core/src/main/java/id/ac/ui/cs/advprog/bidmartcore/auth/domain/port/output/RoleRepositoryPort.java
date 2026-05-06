package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryPort {
    Optional<Role> findById(int roleId);
    Optional<Role> findByName(String name);
    List<Role> findAll();
    Role save(Role role);
    void deleteById(int roleId);
}
