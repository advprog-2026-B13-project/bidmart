package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="permissions")
public class Permission {
    @Id
    @SequenceGenerator(name = "permission_seq_gen", sequenceName = "permission_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permission_seq_gen")
    private int id;

    @Column(name = "name", nullable = false)
    private PermissionValue name;
}
