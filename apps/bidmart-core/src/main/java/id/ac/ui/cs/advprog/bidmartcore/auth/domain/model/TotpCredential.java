package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="totp_credentials")
public class TotpCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne()
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="secret_key", nullable = false)
    private String secretKey;

    @Column(name="is_active", nullable = false)
    private boolean isActive;

    @CreatedDate
    @Column(name="created_at", nullable = false)
    private Instant createdAt;
}
