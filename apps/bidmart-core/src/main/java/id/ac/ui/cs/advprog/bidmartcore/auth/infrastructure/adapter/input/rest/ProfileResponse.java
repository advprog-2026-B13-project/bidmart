package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile payload")
public class ProfileResponse {

    @Schema(description = "User unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @Schema(description = "Display name", example = "Jane Doe")
    private String displayName;

    @Schema(description = "Profile picture URL", example = "https://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "Shipping address", example = "Jl. Margonda Raya No. 100, Depok")
    private String shippingAddress;

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;

    @Schema(description = "Default MFA method", example = "DISABLED")
    private String default2FAMethod;

    @Schema(description = "ISO-8601 account creation timestamp", example = "2026-03-26T09:20:41.756Z")
    private String createdAt;

    @Schema(description = "Role name", example = "USER")
    private String role;

    public static ProfileResponse fromUser(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhotoUrl(),
                user.getShippingAddress(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getDefault2FAMethod() != null ? user.getDefault2FAMethod().name() : null,
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                user.getRole() != null ? user.getRole().getName() : null
        );
    }
}

