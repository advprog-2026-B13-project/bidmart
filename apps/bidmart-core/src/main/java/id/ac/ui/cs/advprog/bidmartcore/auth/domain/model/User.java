package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
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
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="password_hash", nullable = false)
    private String passwordHash;

    @Column(name="display_name")
    private String displayName;

    @Column(name="photo_url")
    private String photoUrl;

    @Column(name="shipping_address", length=200)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name="default_2fa_method", nullable = false)
    private MFAType default2FAMethod;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length=32, nullable = false)
    private UserStatus status;

    @CreatedDate
    @Column(name="created_at", nullable = false)
    private Instant createdAt;
}
