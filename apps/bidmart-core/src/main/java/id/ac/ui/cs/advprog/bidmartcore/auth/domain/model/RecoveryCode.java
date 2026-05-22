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
@Table(name="recovery_codes")
public class RecoveryCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne()
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="code_hash", nullable = false)
    private String codeHash;

    @Column(name="is_used", nullable = false)
    private boolean isUsed;

    @CreatedDate
    @Column(name="created_at", nullable = false)
    private Instant createdAt;
}
