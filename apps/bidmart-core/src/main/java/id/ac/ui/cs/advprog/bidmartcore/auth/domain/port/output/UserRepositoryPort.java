package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

