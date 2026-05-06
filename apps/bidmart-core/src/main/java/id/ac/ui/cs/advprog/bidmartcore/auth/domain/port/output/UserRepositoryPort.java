package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoleId(int roleId);
    UserPage findUsersPage(int page, int size);

    record UserPage(List<User> users, int page, int size, long totalElements, int totalPages, boolean hasNext) {}
}
