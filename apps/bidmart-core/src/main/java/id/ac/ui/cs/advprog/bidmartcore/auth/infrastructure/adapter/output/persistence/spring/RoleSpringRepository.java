package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleSpringRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
    Optional<Role> findByNameIgnoreCase(String name);
}
