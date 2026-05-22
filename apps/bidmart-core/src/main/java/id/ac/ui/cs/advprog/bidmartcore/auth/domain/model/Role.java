package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="roles")
public class Role {
    @Id
    @SequenceGenerator(name = "role_seq_gen", sequenceName = "role_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq_gen")
    private int id;

    @Column(name = "name", length=32, nullable = false)
    private String name;
}
