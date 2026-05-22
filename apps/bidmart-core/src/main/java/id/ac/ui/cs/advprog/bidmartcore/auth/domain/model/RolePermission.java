package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.embeddable.RolePermissionKey;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="role_permissions")
public class RolePermission {
    @EmbeddedId
    private RolePermissionKey id;
}
