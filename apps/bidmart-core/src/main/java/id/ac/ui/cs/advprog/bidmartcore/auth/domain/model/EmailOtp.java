package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="email_otps")
public class EmailOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="otp_hash", nullable = false)
    private String otpHash;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="is_used", nullable = false)
    private boolean isUsed;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;
}
