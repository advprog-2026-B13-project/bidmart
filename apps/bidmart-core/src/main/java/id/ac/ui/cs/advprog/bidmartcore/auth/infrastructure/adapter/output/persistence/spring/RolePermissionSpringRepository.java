package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.RolePermission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.embeddable.RolePermissionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionSpringRepository extends JpaRepository<RolePermission, RolePermissionKey> {

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.role.id = :roleId")
    List<RolePermission> findAllByRoleId(@Param("roleId") int roleId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.id.role.id = :roleId")
    void deleteAllByRoleId(@Param("roleId") int roleId);
}
