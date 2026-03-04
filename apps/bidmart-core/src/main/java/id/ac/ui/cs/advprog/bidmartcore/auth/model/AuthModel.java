package id.ac.ui.cs.advprog.bidmartcore.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// TODO: rename atau modif file template ini
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="auth")
public class AuthModel {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(name="SAMPLE_COLUMN", length=50)
    private String sampleColumn;
}
