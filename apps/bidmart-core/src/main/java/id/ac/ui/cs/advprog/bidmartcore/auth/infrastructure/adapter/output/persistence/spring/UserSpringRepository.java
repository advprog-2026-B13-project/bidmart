package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSpringRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.id = :roleId")
    long countByRoleId(@Param("roleId") int roleId);
}
