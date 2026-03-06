package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="sessions")
public class Session implements Serializable {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="refresh_token", nullable = false)
    private String refreshToken;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="is_active", nullable = false)
    private boolean isActive;
}
