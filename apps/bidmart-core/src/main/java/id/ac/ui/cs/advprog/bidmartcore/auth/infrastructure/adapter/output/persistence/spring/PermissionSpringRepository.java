package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionSpringRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByName(PermissionValue permissionValue);
}

