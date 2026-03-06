package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.embeddable;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionKey implements Serializable {
    @ManyToOne
    @JoinColumn(name="role_id", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name="permission_id", nullable = false)
    private Permission permission;
}
